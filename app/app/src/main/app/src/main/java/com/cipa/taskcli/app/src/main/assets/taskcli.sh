#!/system/bin/sh
# TaskCLI - Android compatible

HOME="${HOME:-/data/data/com.cipa.taskcli/files}"
DATA_DIR="$HOME/.taskcli"
DATA_FILE="$DATA_DIR/tasks.tsv"
mkdir -p "$DATA_DIR"
touch "$DATA_FILE"

next_id() {
    if [ ! -s "$DATA_FILE" ]; then echo 1; return; fi
    awk -F'	' 'BEGIN{m=0} {if($1+0>m) m=$1+0} END{print m+1}' "$DATA_FILE"
}

cmd_add() {
    text="$*"
    if [ -z "$text" ]; then echo "error: task kosong"; return; fi
    id=$(next_id)
    ts=$(date +"%Y-%m-%d %H:%M:%S")
    printf '%s	0	%s	%s\n' "$id" "$ts" "$text" >> "$DATA_FILE"
    echo "[OK] tambah #$id: $text"
}

cmd_list() {
    filter="${1:-all}"
    if [ ! -s "$DATA_FILE" ]; then echo "belum ada task."; return; fi

    total=$(wc -l < "$DATA_FILE" | tr -d ' ')
    done_count=$(awk -F'	' '$2==1' "$DATA_FILE" | wc -l | tr -d ' ')
    pending=$((total - done_count))

    echo "--- Tasks ($pending pending / $done_count done) ---"
    while IFS='	' read -r id status ts text; do
        case "$filter" in
            pending) [ "$status" = "1" ] && continue ;;
            done)    [ "$status" = "0" ] && continue ;;
        esac
        if [ "$status" = "1" ]; then
            echo "  [x] #$id $text"
        else
            echo "  [ ] #$id $text"
        fi
    done < "$DATA_FILE"
    echo
}

cmd_done() {
    id="$1"
    if [ -z "$id" ]; then echo "error: butuh id"; return; fi
    if ! grep -q "^${id}	" "$DATA_FILE"; then echo "error: task #$id ga ada"; return; fi
    tmp="$DATA_FILE.tmp"
    awk -F'	' -v OFS='	' -v id="$id" '$1==id{$2=1} {print}' "$DATA_FILE" > "$tmp"
    mv "$tmp" "$DATA_FILE"
    echo "[OK] task #$id selesai"
}

cmd_rm() {
    id="$1"
    if [ -z "$id" ]; then echo "error: butuh id"; return; fi
    if ! grep -q "^${id}	" "$DATA_FILE"; then echo "error: task #$id ga ada"; return; fi
    tmp="$DATA_FILE.tmp"
    awk -F'	' -v OFS='	' -v id="$id" '$1!=id {print}' "$DATA_FILE" > "$tmp"
    mv "$tmp" "$DATA_FILE"
    echo "[OK] task #$id dihapus"
}

cmd_stats() {
    if [ ! -s "$DATA_FILE" ]; then echo "belum ada task."; return; fi
    total=$(wc -l < "$DATA_FILE" | tr -d ' ')
    done_count=$(awk -F'	' '$2==1' "$DATA_FILE" | wc -l | tr -d ' ')
    pending=$((total - done_count))
    percent=$(( done_count * 100 / total ))
    echo "--- Stats ---"
    echo "  total   : $total"
    echo "  pending : $pending"
    echo "  selesai : $done_count"
    echo "  progres : $percent%"
}

cmd_help() {
    echo "TaskCLI - commands:"
    echo "  add <teks>        tambah task"
    echo "  list [filter]     list (all|pending|done)"
    echo "  done <id>         tandai selesai"
    echo "  rm <id>           hapus task"
    echo "  stats             statistik"
    echo "  help              bantuan ini"
}

cmd="${1:-help}"
shift 2>/dev/null
case "$cmd" in
    add|a)          cmd_add "$@" ;;
    list|ls|l)      cmd_list "$@" ;;
    done|d)         cmd_done "$@" ;;
    rm|del|remove)  cmd_rm "$@" ;;
    stats|st)       cmd_stats ;;
    help|h|*)       cmd_help ;;
esac
