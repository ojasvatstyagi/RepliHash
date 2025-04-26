export COUNTER_FILE="$(mktemp)"
echo 0 > "$COUNTER_FILE"

git filter-branch --env-filter '
OLD_EMAIL1="davide.pedranz@gmail.com"
OLD_EMAIL2="andrea.zorzi.94@gmail.com"
OLD_EMAIL3="pedranz@fbk.eu"
NEW_EMAIL="ojastyagi753@gmail.com"
NEW_NAME="Ojas Tyagi"

if [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL1" ] || [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL2" ] || [ "$GIT_COMMITTER_EMAIL" = "$OLD_EMAIL3" ]; then
    export GIT_COMMITTER_EMAIL="$NEW_EMAIL"
    export GIT_COMMITTER_NAME="$NEW_NAME"
fi
if [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL1" ] || [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL2" ] || [ "$GIT_AUTHOR_EMAIL" = "$OLD_EMAIL3" ]; then
    export GIT_AUTHOR_EMAIL="$NEW_EMAIL"
    export GIT_AUTHOR_NAME="$NEW_NAME"
fi

COUNTER=$(cat "$COUNTER_FILE")
echo $((COUNTER + 1)) > "$COUNTER_FILE"

# Start on Feb 15, 2025
START_YEAR=2025
START_MONTH=2
START_DAY=15
TOTAL_DAYS=$((COUNTER))
DAY=$((START_DAY + TOTAL_DAYS - 1))  # -1 because first commit is start day

# Function to get days in month (2025 is not a leap year)
get_days_in_month() {
    MONTH=$1
    case $MONTH in
        2) echo 28 ;;
        4|6|9|11) echo 30 ;;
        *) echo 31 ;;
    esac
}

# Calculate month and day
MONTH=$START_MONTH
while true; do
    DAYS_IN_MONTH=$(get_days_in_month $MONTH)
    if [ $DAY -le $DAYS_IN_MONTH ]; then
        break
    fi
    DAY=$((DAY - DAYS_IN_MONTH))
    MONTH=$((MONTH + 1))
done

# Format the date
export GIT_COMMITTER_DATE="2025-$(printf "%02d" $MONTH)-$(printf "%02d" $DAY)T00:00:00"
export GIT_AUTHOR_DATE="2025-$(printf "%02d" $MONTH)-$(printf "%02d" $DAY)T00:00:00"
' --tag-name-filter cat -- --all

rm "$COUNTER_FILE"
