#!/bin/bash

declare -A col
declare -A users
declare -A configs
declare -A environments
declare -A statuses_seen
declare -A test_cases_by_suite
declare -A test_titles_by_suite
declare -A test_spec_data_by_suite

show_help() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS] path-to-tsv

This script automates the generation of test files for a project.
It reads a .tsv file and creates the necessary fixtures, pages, and tests files, as well as a {project-name}.config.ts file.
If a .env file is present in the directory, its values will be used to generate {project-name}.config.ts.

OPTIONS:
  -h, --help                Display this help message
  -n, --project-name NAME   Specify the project name (default "project")
  -e, --ignore-env          Ignore the .env file if present in the project directory
  -s, --status VALUE        Only generate rows whose Test Status is VALUE
                            (default "Needs Automation")
  -a, --auth-storage PATH   For rows flagged as needing authentication, add
                            dependencies: ['setup'] and use.storageState: 'PATH' to the
                            generated config. Omit it and no auth wiring is emitted.

ARGUMENTS:
  path-to-tsv               Path to the TSV file (required)

Columns are resolved by HEADER NAME, not by position, so the sheet can be reordered or gain
new columns without breaking the script. The header is the first row that holds both a suite
column and a test name column; every row above it is skipped, as is every row with an empty
suite or test name (section titles, spacers).

Recognised headers, case and accent insensitive:

  Context | Describe | Module    (REQUIRED) => The test suite title, used as the first
                                               argument for test.describe(). Split on "/"
                                               into nested folders. This is the only source
                                               of folder names.
  Test Description | Test Case Name          => The name of individual test cases, used as
     | Test Name                  (REQUIRED)    the first argument for test().
  Test Status | Status                       => Compared against --status. With no such
                                               column, every row is generated.
  Needs Authentication                       => Truthy values are yes, y, true, 1, sim and s.
                                               Only acted on together with --auth-storage.
  Steps | BDD                                => The sequence of actions needed to execute
                                               the test, written as a comment in test().
  Comments | Notes                           => Additional information, written as # NOTES.

Every row sharing a suite lands in the same place: one spec file holding all of its test
cases, one fixture and one page object. A suite of "Bookshop / Listing" produces:

  pages/bookshop/listing/ListingPage.ts
  fixtures/bookshopListingTest.ts
  tests/bookshop/listing/config.ts
  tests/bookshop/listing/listing.spec.ts

EXAMPLES:
  $(basename "$0") data.tsv
  $(basename "$0") -n "My Project" data.tsv
  $(basename "$0") --project-name "My Project" data.tsv
  $(basename "$0") -n "My Project" --status "To Automate" data.tsv
  $(basename "$0") -n "My Project" --auth-storage .auth/user.json data.tsv
EOF
}

escape_single_quote() {
  echo "${1//\'/\\\'}"
}

trim() {
  echo "$1" | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//'
}

# Folds accented Latin characters onto ASCII by rewriting their UTF-8 byte pairs. Done under
# LC_ALL=C so the result never depends on the caller's locale: with a UTF-8 collation
# "[^a-z]" keeps accented letters, without one it drops them, and iconv --TRANSLIT turns
# them into "?".
transliterate() {
  LC_ALL=C sed -E '
    s/\xc3[\x80-\x85]/A/g; s/\xc3\x86/AE/g; s/\xc3\x87/C/g
    s/\xc3[\x88-\x8b]/E/g; s/\xc3[\x8c-\x8f]/I/g
    s/\xc3\x90/D/g; s/\xc3\x91/N/g; s/\xc3[\x92-\x96]/O/g; s/\xc3\x98/O/g
    s/\xc3[\x99-\x9c]/U/g; s/\xc3\x9d/Y/g; s/\xc3\x9e/TH/g; s/\xc3\x9f/ss/g
    s/\xc3[\xa0-\xa5]/a/g; s/\xc3\xa6/ae/g; s/\xc3\xa7/c/g
    s/\xc3[\xa8-\xab]/e/g; s/\xc3[\xac-\xaf]/i/g
    s/\xc3\xb0/d/g; s/\xc3\xb1/n/g; s/\xc3[\xb2-\xb6]/o/g; s/\xc3\xb8/o/g
    s/\xc3[\xb9-\xbc]/u/g; s/\xc3\xbd/y/g; s/\xc3\xbe/th/g; s/\xc3\xbf/y/g
  ' <<< "$1"
}

normalize_string() {
  local output="" input word
  input=$(transliterate "$1")

  for word in $(LC_ALL=C sed -E 's|[_/-]| |g; s/[^a-zA-Z0-9 ]//g' <<< "${input,,}"); do
    output+="${word^}"
  done

  echo "${output,}"
}

# TypeScript identifiers cannot start with a digit, so a suite such as "2FA / Login" would
# otherwise emit "export const 2faLoginTest".
to_identifier() {
  if [[ "$1" == [0-9]* ]]; then
    echo "_$1"
  else
    echo "$1"
  fi
}

is_truthy() {
  case "$(trim "${1,,}")" in
    yes|y|true|1|sim|s) return 0 ;;
    *) return 1 ;;
  esac
}

add_test_case() {
  test_cases_by_suite["$path_suffix"]+=$(
    {
      echo "test('$(escape_single_quote "$test_name")', async ({ ${pom_name,} }) => {"

      if [[ -n $(trim "$notes") || -n $(trim "$steps") ]]; then
        echo "/**"

        if [[ -n $(trim "$notes") ]]; then
          echo -e "\t\t\t# NOTES"
          while IFS= read -r note_line; do
            echo -e "\t\t\t# $note_line"
          done <<< "$(echo "$notes" | fold -s -w 60)"
          echo ""
        fi

        while IFS= read -r step_line; do
          echo -e "\t\t\t$step_line"
        done <<< "$(echo "$steps" | sed 's/\([[:space:]]\)\(And\|When\|Then\)\([[:space:]]\)/\1\n\2\3/g')"

        echo -e "\t\t*/"
      fi

      echo "});"
    }
  )$'\n'
}

match_column_name() {
  local cell
  cell=$(transliterate "$1")
  cell=$(trim "${cell,,}")

  case "$cell" in
    context*|describe*|module*|modulo*|feature*|funcionalidade*) echo "suite" ;;
    "test description"*|"test case name"*|"test name"*|"caso de teste"*) echo "test_name" ;;
    "test status"*|status*|situacao*) echo "status" ;;
    *authentication*|*autenticacao*) echo "auth" ;;
    steps*|bdd*|passos*) echo "steps" ;;
    comments*|notes*|observacoes*|notas*) echo "notes" ;;
  esac
}

# Fills col[] from one row and reports whether that row looks like the header, which is what
# lets the script skip any number of guide/title rows above it.
map_header_row() {
  local cell key
  local index=0

  unset col
  declare -gA col

  while IFS= read -r cell; do
    index=$((index + 1))
    key=$(match_column_name "$cell")

    if [[ -n "$key" && -z "${col[$key]}" ]]; then
      col["$key"]=$index
    fi
  done < <(tr '\t' '\n' <<< "${1//$'\r'/}")

  [[ -n "${col[suite]}" && -n "${col[test_name]}" ]]
}

resolve_columns() {
  local header_line
  local row=0

  header_row=0

  while IFS= read -r header_line || [[ -n "$header_line" ]]; do
    row=$((row + 1))

    if map_header_row "$header_line"; then
      header_row=$row
      break
    fi
  done < "$file_path"

  if (( header_row == 0 )); then
    echo "No header row found in $file_path."
    echo "It needs a suite column (Context, Describe or Module) and a test name column"
    echo "(Test Description, Test Case Name or Test Name). Run with --help for the full list."
    exit 1
  fi
}

get_column() {
  if [[ -z "${col[$1]}" ]]; then
    return
  fi

  echo "$line" | cut -f"${col[$1]}"
}

build_path_segments() {
  local part normalized
  local -a segments=()

  path_suffix=""
  fixture_name=""
  normalized_describe=""

  IFS='/' read -ra segments <<< "$suite"

  for part in "${segments[@]}"; do
    normalized=$(normalize_string "$part")

    if [[ -z "$normalized" ]]; then
      continue
    fi

    if [[ -z "$path_suffix" ]]; then
      path_suffix="$normalized"
      fixture_name="$normalized"
    else
      path_suffix+="/$normalized"
      fixture_name+="${normalized^}"
    fi

    normalized_describe="$normalized"
  done

  fixture_name=$(to_identifier "$fixture_name")
}

define_columns_and_dir() {
  line="${line//$'\r'/}"

  suite=$(get_column suite)
  test_name=$(get_column test_name)
  test_status=$(get_column status)
  needs_authentication=$(get_column auth)
  steps=$(get_column steps)
  notes=$(get_column notes)

  test_title=$(trim "$suite")

  build_path_segments
}

create_POM_file() {
  pom_dir="pages/${path_suffix}"
  pom_name=$(to_identifier "${normalized_describe^}Page")

  mkdir -p "$pom_dir"

  {
    echo "import { Page } from '@playwright/test';"
    echo ""
    echo "export class ${pom_name} {"
    echo "  constructor(readonly page: Page) {}"
    echo "}"
  } > "$pom_dir/$pom_name.ts"
}

create_fixture_file() {
  mkdir -p "fixtures"

  {
    echo "import { test } from '@playwright/test';"
    echo "import { $pom_name } from '../$pom_dir/$pom_name';"
    echo ""
    echo "interface ${fixture_name^}Fixture {"
    echo "  ${pom_name,}: ${pom_name};"
    echo "}"
    echo ""
    echo "export const ${fixture_name}Test = test.extend<${fixture_name^}Fixture>({"
    echo "  ${pom_name,}: async ({ page }, use) => {"
    echo "    await use(new ${pom_name}(page));"
    echo "  }"
    echo "});"
  } > "fixtures/${fixture_name}Test.ts"
}

create_config_file() {
  local dir="tests/${path_suffix}"

  configs["$fixture_name"]="$dir"
  test_titles_by_suite["$path_suffix"]="$test_title"
  test_spec_data_by_suite["$path_suffix"]+="$fixture_name,$dir,${normalized_describe}"$'\n'

  mkdir -p "$dir"

  {
    echo "import { Project } from '@playwright/test';"
    echo ""
    echo "export const config: Project = {"
    echo "  name: '${fixture_name}',"
    echo "  testDir: '$dir',"
    # Scoped to this suite's own spec so a parent testDir does not collect the spec files of
    # nested suites, which would run them once per project.
    echo "  testMatch: '${normalized_describe}.spec.ts',"

    if [[ -n "$auth_storage" ]] && is_truthy "$needs_authentication"; then
      echo "  dependencies: ['setup'],"
      echo "  use: {"
      echo "    storageState: '$(escape_single_quote "$auth_storage")',"
      echo "  },"
    fi

    echo "}"
  } > "$dir/config.ts"
}

create_spec_file() {
  for suite_path in "${!test_cases_by_suite[@]}"; do
    IFS=, read -r fixture_name test_spec_dir file_name <<< "${test_spec_data_by_suite[$suite_path]}"
    folder_level=$(( $(echo "$test_spec_dir" | tr -cd "/" | wc -c) + 1 ))
    level_string=$(printf '../%.0s' $(seq 1 "$folder_level"))
    fixture_dir="'${level_string}fixtures/${fixture_name}Test'"

    {
      echo "import { ${fixture_name}Test as test } from $fixture_dir;"
      echo ""
      echo "test.describe('$(escape_single_quote "${test_titles_by_suite[$suite_path]}")', () => {"
      echo "${test_cases_by_suite[$suite_path]}"
      echo "});"
    } > "$test_spec_dir/$file_name.spec.ts"
  done
}

read_env_file() {
  if $ignore_env; then
    echo -e "❌ \e[3;2mIgnoring .env file\e[0m"
    return
  elif [[ -f ".env" && -s ".env" ]]; then
    echo -e "🔑 \e[3;2mReading .env file\e[0m"
    mapfile -t env_lines < .env
    for env_line in "${env_lines[@]}"; do
      original_key="${env_line%%=*}"

      if [[ "$original_key" == *"BASE_URL"* || "$original_key" == *"_URL" ]]; then
        normalized_key=$(normalize_string "$original_key")
        environments["$normalized_key"]="$original_key"
      elif [[ -n "$original_key" ]]; then
        key=$(echo "$original_key" | sed -E 's/(_PASSWORD|_EMAIL)$//')
        normalized_key=$(normalize_string "$key")
        users["$normalized_key"]+="$original_key,"
      fi
    done
  elif [[ ! -s ".env" ]]; then
    echo -e "❌ \e[3;2m.env file found, but ignored because it's empty\e[0m"
  else
    echo -e "❌ \e[3;2mNo .env file found\e[0m"
  fi
}

create_project_config() {
  if $ignore_env; then
    return
  fi

  echo -e "📐 \e[3;2mCreating the ${project_name^} config file\e[0m"
 {
    echo "import { loadEnvFile } from 'process';"
    echo ""
    echo "loadEnvFile(process.cwd() + '/.env');"
    echo ""
    echo "export const ${normalized_project_name}Config = {"
    echo "  environments: {"

    for environment in "${!environments[@]}"; do
      echo "    ${environment,}: process.env.${environments["$environment"]}!,"
    done

    echo "},"
    echo ""
    echo "  users: {"

    for user in "${!users[@]}"; do
      IFS=, read -r first second _ <<< "${users["$user"]}"

      echo "    ${user,}: {"

      for credential in "$first" "$second"; do
        if [[ -n "$credential" ]]; then
          credential_key="${credential##*_}"
          echo "      ${credential_key,,}: process.env.${credential}!,"
        fi
      done

      echo "    },"
    done

    echo "  }"
    echo "};"
  } > "${normalized_project_name}.config.ts"
}

create_playwright_config() {
  has_base_url="${environments["baseUrl"]}"
  sorted_configs=($(printf "%s\n" "${!configs[@]}" | sort))
  {
    echo "import { defineConfig } from '@playwright/test';"

    if [ -n "$has_base_url" ]; then
      echo "import { ${normalized_project_name}Config } from './${normalized_project_name}.config';"
    fi
    echo ""

    for config in "${sorted_configs[@]}"; do
      echo "import { config as ${config} } from './${configs["$config"]}/config';"
    done

    echo ""
    echo "export default defineConfig({"
	  echo "  testDir: './tests',"
    echo ""
  	echo "  reporter: ["
		echo "    ['html', { open: 'never',}],],"
    echo ""
    echo "  use: {"

    if [ -n "$has_base_url" ]; then
      echo "   baseURL: ${normalized_project_name}Config.environments.baseUrl,"
    fi

    echo "   screenshot: 'only-on-failure',"
    echo "   trace: 'on-first-retry',"
    echo "},"
    echo ""
    echo "  projects: ["

    for config in "${sorted_configs[@]}"; do
      echo "    $config,"
    done

    echo "  ],"
    echo "});"
  } > "playwright.config.ts"
}

format_files() {
  prettier_bin="./node_modules/.bin/prettier"
  local -a targets=("{tests,pages,fixtures}/**/*.ts" "playwright.config.ts")

  # Prettier errors out on a pattern that matches nothing, and the project config is absent
  # whenever the .env file was ignored.
  if [[ -f "${normalized_project_name}.config.ts" ]]; then
    targets+=("${normalized_project_name}.config.ts")
  fi

  if [[ -x "$prettier_bin" ]]; then
    echo -e "✨ \e[3;2mFormatting the generated files with Prettier\e[0m"
    "$prettier_bin" "${targets[@]}" --write --config ./.prettierrc > /dev/null
  fi
}

report_empty_result() {
  echo -e "🛑 \e[1;33mNo rows were generated.\e[0m"

  if [[ -z "${col[status]}" ]]; then
    echo -e "   \e[3;2mEvery row had an empty suite or test name.\e[0m"
    return
  fi

  echo -e "   \e[3;2mNo row has a Test Status of \"$status_filter\". Values found:\e[0m"

  for status in "${!statuses_seen[@]}"; do
    echo -e "   \e[3;2m- \"$status\"\e[0m"
  done

  echo -e "   \e[3;2mPick one of them with --status.\e[0m"
}

read_file() {
  echo -e "📚 \e[3;2mReading the .tsv file\e[0m"
  resolve_columns
  generated_cases=0
  # The || [[ -n "$line" ]] keeps the last row when the .tsv has no trailing newline, which
  # is how spreadsheets usually export it.
  while IFS= read -r line || [[ -n "$line" ]]; do
    define_columns_and_dir

    if [[ -z $(trim "$suite") || -z $(trim "$test_name") ]]; then
      continue
    fi

    if [[ -n "${col[status]}" ]]; then
      statuses_seen["$(trim "$test_status")"]=1

      if [[ "$(trim "$test_status")" != "$status_filter" ]]; then
        continue
      fi
    fi

    if [[ -z "$path_suffix" ]]; then
      echo -e "\r⚠️  \e[3;2mSkipping \"$test_name\": no usable characters in its suite name\e[0m\e[K"
      continue
    fi

    echo -ne "\r📝 \e[3;2mWriting "$test_title"...\e[0m\e[K"
    create_POM_file
    create_fixture_file
    create_config_file
    add_test_case
    generated_cases=$((generated_cases + 1))
  done < <(tail -n +$((header_row + 1)) "$file_path")
  echo -ne "\r\e[K"
  create_spec_file
}

generate_test_files() {
  echo -e "\n\e[1mTest Files Generator - Upgrades Team @ \e[34mLiferay\e[0m\n"

  cd "$(git rev-parse --show-toplevel)" || exit 1

  ignore_env=false
  status_filter="Needs Automation"
  auth_storage=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      -n|--project-name)
        project_name="${2^}"
        normalized_project_name="$(normalize_string "$project_name")"
        shift 2
        ;;
      -e|--ignore-env)
        ignore_env=true
        shift 1
        ;;
      -s|--status)
        status_filter="$2"
        shift 2
        ;;
      -a|--auth-storage)
        auth_storage="$2"
        shift 2
        ;;
      -h|--help)
        show_help
        exit 0
        ;;
      *)
        if [[ -z "$file_path" ]]; then
          file_path="$1"
        fi
        shift
        ;;
    esac
  done

  if [[ -z "$file_path" || ! "$file_path" == *.tsv ]]; then
    echo "Please, input a path to a TSV (.tsv) file."
    exit 1
  fi

  if [[ ! -f "$file_path" ]]; then
    echo "File not found."
    exit 1
  fi

  if [[ -z "$project_name" ]]; then
    echo -e "🛑 \e[1;33mNo project name detected. Please, enter a project name: \e[0m\c"
    read user_input
    upper_case="${user_input^}"
    project_name=${upper_case:-"project"}
    normalized_project_name=$(normalize_string "$project_name")
    tput cuu1
    echo -ne "\r"
    tput el
    tput cuu1
  fi

  read_file

  if (( generated_cases == 0 )); then
    report_empty_result
    exit 1
  fi

  read_env_file
  create_project_config
  create_playwright_config
  format_files

  echo -e "\n🎉 \e[1;32mSuccessfully created the $project_name test files: ${#test_cases_by_suite[@]} suites, $generated_cases test cases\e[0m"
}

generate_test_files "$@"

