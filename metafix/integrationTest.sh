#! /bin/bash

set -e

cd "$(dirname "$(readlink -f "$0")")"

metafix_file=test.flux
catmandu_file=test.cmd

fix_file=test.fix
disabled_file=disabled.txt

input_glob=input.*
expected_glob=expected.*

metafix_output_glob=output-metafix.*
catmandu_output_glob=output-catmandu.*

root_directory="$PWD"
data_directory="$root_directory/src/test/resources/org/metafacture/metafix/integration"
gradle_command="$root_directory/../gradlew"

[ -t 1 -a -x /usr/bin/colordiff ] && colordiff=colordiff || colordiff=cat

function _tput() {
  tput -T "${TERM:-dumb}" "$@" || true
}

color_error=$(_tput setaf 1)                # red
color_failed=$(_tput setaf 1)               # red
color_failure=$(_tput bold)$(_tput setaf 1) # bold red
color_info=$(_tput setaf 5)                 # purple
color_invalid=$(_tput setaf 6)              # cyan
color_passed=$(_tput setaf 2)               # green
color_reset=$(_tput sgr0)                   # reset
color_skipped=$(_tput setaf 3)              # yellow
color_success=$(_tput bold)$(_tput setaf 2) # bold green
color_test=$(_tput bold)                    # bold

declare -A tests

failed=0
invalid=0
passed=0
skipped=0

current_file=

function log() {
  echo "$@"
}

function warn() {
  echo "$@" >&2
}

function die() {
  [ $# -gt 0 ] && warn "$@"
  exit 2
}

function run_metafix() {
  $gradle_command -p "$root_directory" :metafix-runner:run --args="$1"
}

function run_catmandu() {
  :
}

function get_file() {
  local test=$1 type=$2 reason; shift 2

  if [ $# -ne 1 ]; then
    reason="Ambiguous $type files: $*"
  elif [ ! -r "$1" ]; then
    reason="No $type file: $1"
  else
    current_file=$1
    return 0
  fi

  log "$color_test$test$color_reset: ${color_invalid}INVALID$color_reset ($reason)"

  ((invalid++)) || true

  return 1
}

function command_info() {
  log "  ${color_info}${1^} command exit status$color_reset: $2"

  [ -s "$3" ] && log "  ${color_info}${1^} command output$color_reset: $3" || rm -f "$3"
  [ -s "$4" ] && log "  ${color_info}${1^} command error$color_reset:  $4" || rm -f "$4"
}

function run_tests() {
  local test matched=1\
    test_input test_expected test_disabled\
    metafix_command_output metafix_command_error\
    metafix_exit_status metafix_output metafix_diff

  cd "$data_directory"

  for test in $(find */ -type f -path "$1/$metafix_file" -printf "%h\n"); do
    matched=0

    [ -n "${tests[$test]}" ] && continue || tests[$test]=1

    test_directory="$PWD/$test"

    get_file "$test" Fix "$test_directory"/$fix_file || { log; continue; }
    test_fix=$current_file

    get_file "$test" input "$test_directory"/$input_glob || { log; continue; }
    test_input=$current_file

    get_file "$test" expected "$test_directory"/$expected_glob || { log; continue; }
    test_expected=$current_file

    test_disabled="$test_directory/$disabled_file"

    if [ -r "$test_disabled" ]; then
      log "$color_test$test$color_reset: ${color_skipped}SKIPPED$color_reset ($(<"$test_disabled"))"

      ((skipped++)) || true
    else
      metafix_command_output="$test_directory/metafix.out"
      metafix_command_error="$test_directory/metafix.err"

      # TODO: catmandu (optional)

      if run_metafix "$test_directory/$metafix_file" >"$metafix_command_output" 2>"$metafix_command_error"; then
        metafix_exit_status=$?

        if get_file "$test" output "$test_directory"/$metafix_output_glob; then
          metafix_output=$current_file
          metafix_diff="$test_directory/metafix.diff"

          if diff -u "$test_expected" "$metafix_output" >"$metafix_diff"; then
            #log "$color_test$test$color_reset: ${color_passed}PASSED$color_reset"

            rm -f "$metafix_diff" "$metafix_command_output" "$metafix_command_error"

            ((passed++)) || true

            #log
          else
            log "$color_test$test$color_reset: ${color_failed}FAILED$color_reset"

            log "  Fix:      $test_fix"
            log "  Input:    $test_input"
            log "  Expected: $test_expected"
            log "  Output:   $metafix_output"
            log "  Diff:     $metafix_diff"

            [ -s "$metafix_diff" ] && $colordiff <"$metafix_diff" || rm -f "$metafix_diff"

            command_info metafix "$metafix_exit_status" "$metafix_command_output" "$metafix_command_error"

            ((failed++)) || true

            log
          fi
        else
          command_info metafix "$metafix_exit_status" "$metafix_command_output" "$metafix_command_error"

          log
        fi
      else
        metafix_exit_status=$?

        log "$color_test$test$color_reset: ${color_error}ERROR$color_reset"

        command_info metafix "$metafix_exit_status" "$metafix_command_output" "$metafix_command_error"

        ((failed++)) || true

        log
      fi
    fi
  done

  cd - >/dev/null

  return $matched
}

if [ $# -eq 0 ]; then
  run_tests '*' || true
else
  for pattern in "$@"; do
    run_tests "$pattern" || warn "No tests matching pattern: $pattern"
  done
fi

[ ${#tests[@]} -gt 0 ] || die "No tests found: $data_directory"

summary="${color_passed}$passed passed$color_reset"
summary+=", ${color_failed}$failed failed$color_reset"
summary+=", ${color_skipped}$skipped skipped$color_reset"
summary+=", ${color_invalid}$invalid invalid$color_reset"

if [ $failed -gt 0 -o $invalid -gt 0 ]; then
  log "${color_failure}FAILURE$color_reset: $summary"
  exit 1
else
  log "${color_success}SUCCESS$color_reset: $summary"
  exit 0
fi