#!/usr/bin/env sh
# Recreate the agent-tool symlinks under .claude/.
#
# Source of truth for all agent assets is the agent-agnostic .agents/ tree:
#   .agents/skills/   .agents/commands/   .agents/agents/
# .claude/ is gitignored (it is Claude Code-specific), so these symlinks do NOT
# survive `git clone`. Run this once after cloning (and it is safe to re-run).
set -eu

# cd to repo root (this script lives in scripts/)
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"

mkdir -p .claude

# link <name>: point .claude/<name> at ../.agents/<name>, replacing whatever is
# there. Handles a leftover real directory (e.g. from an older checkout where
# .claude/skills was tracked) so we never create a symlink *inside* it.
link() {
  name="$1"
  target=".claude/$name"
  if [ -L "$target" ] || [ -e "$target" ]; then
    rm -rf "$target"
  fi
  ln -s "../.agents/$name" "$target"
}

link skills
link commands
link agents

echo "Linked .claude/{skills,commands,agents} -> ../.agents/*"
