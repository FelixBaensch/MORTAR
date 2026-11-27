#!/usr/bin/env bash
# Checks the output of an installer smoke test (see .github/workflows/deployment.yml) after MORTAR was started and
# stopped again. Fails if
#   - the console output contains an uncaught exception or a SEVERE error (MORTAR logs to the console only until its
#     logging is set up),
#   - MORTAR wrote no log file,
#   - the log file contains a SEVERE error, or
#   - the log file lacks the line MainApp.start() logs after the main window was shown, i.e. startup did not complete
#     (e.g. it hangs or blocks in a dialog after a problem that was only logged as a WARNING).
# The log file is always printed, so the startup log can also be inspected on green runs.
#
# Usage: check-smoke-test-logs.sh <console output file> <MORTAR log folder>
set -uo pipefail

tmpConsoleLog="$1"
tmpLogDir="$2"
# must match the message logged at the end of MainApp.start()
tmpStartupCompletedLine="INFO: MORTAR startup completed"

# Fails with the given message if the given extended regular expression matches the given file. Also fails if the file
# cannot be read, since grep then returns 2 and an unreadable file must not count as "no match".
fail_if_matches() {
    grep -qE "$1" "$2"
    case $? in
        0) echo "::error::$3"; exit 1 ;;
        1) ;;
        *) echo "::error::Could not read $2"; exit 1 ;;
    esac
}

fail_if_matches "Exception in thread|^SEVERE:" "$tmpConsoleLog" "MORTAR printed an uncaught exception or a SEVERE error"
tmpLogFile=$(ls "$tmpLogDir"/MORTAR_Log_*.txt 2> /dev/null | head -n 1)
if [ -z "$tmpLogFile" ]; then
    echo "::error::MORTAR wrote no log file to $tmpLogDir"
    exit 1
fi
echo "----- $tmpLogFile -----"
cat "$tmpLogFile"
echo "-----"
fail_if_matches "^SEVERE:" "$tmpLogFile" "MORTAR logged a SEVERE error during startup (see the log above)"
if ! grep -qF "$tmpStartupCompletedLine" "$tmpLogFile"; then
    echo "::error::MORTAR did not complete its startup within 30 s (no \"$tmpStartupCompletedLine\" in the log above)"
    exit 1
fi
echo "Smoke test passed"
