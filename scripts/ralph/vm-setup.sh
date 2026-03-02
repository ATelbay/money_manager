#!/bin/bash
# Ralph VM Setup — installs all dependencies needed to run Ralph on the Garlic GCP VM.
# Target: Ubuntu, e2-medium (4GB RAM), /home/node user
#
# Run once on the VM:
#   chmod +x scripts/ralph/vm-setup.sh && ./scripts/ralph/vm-setup.sh
#
# After this script, manually complete:
#   1. claude  (follow prompts to authenticate with Anthropic API key)
#   2. gh auth login  (follow prompts to authenticate with GitHub)
#   3. Copy app/google-services.json to the repo on the VM (it's gitignored)

set -e

ANDROID_HOME="$HOME/android-sdk"
REPO_DIR="/home/node/.openclaw/workspace/money_manager"

echo ""
echo "================================================="
echo "  Ralph VM Setup"
echo "================================================="
echo ""

# ── 1. System packages ────────────────────────────────
echo "[1/6] Installing system packages..."
sudo apt-get update -q
sudo apt-get install -y -q \
  git \
  jq \
  curl \
  unzip \
  wget \
  openjdk-17-jdk

echo "      Java version: $(java -version 2>&1 | head -1)"

# ── 2. Node.js LTS ───────────────────────────────────
echo ""
echo "[2/6] Installing Node.js..."
if ! command -v node &> /dev/null; then
  curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash - -q
  sudo apt-get install -y -q nodejs
fi
echo "      Node: $(node --version), npm: $(npm --version)"

# ── 3. Claude CLI ────────────────────────────────────
echo ""
echo "[3/6] Installing Claude CLI..."
npm install -g @anthropic-ai/claude-code --quiet
echo "      Claude CLI: $(claude --version 2>/dev/null || echo 'installed')"

# ── 4. GitHub CLI ────────────────────────────────────
echo ""
echo "[4/6] Installing GitHub CLI..."
if ! command -v gh &> /dev/null; then
  sudo mkdir -p -m 755 /etc/apt/keyrings
  wget -qO- https://cli.github.com/packages/githubcli-archive-keyring.gpg \
    | sudo tee /etc/apt/keyrings/githubcli-archive-keyring.gpg > /dev/null
  sudo chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] \
    https://cli.github.com/packages stable main" \
    | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null
  sudo apt-get update -q
  sudo apt-get install -y -q gh
fi
echo "      gh: $(gh --version | head -1)"

# ── 5. Android SDK ───────────────────────────────────
echo ""
echo "[5/6] Installing Android SDK (compileSdk 36)..."

mkdir -p "$ANDROID_HOME/cmdline-tools"

if [ ! -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
  echo "      Downloading Android command-line tools..."
  wget -q \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" \
    -O /tmp/cmdline-tools.zip

  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools-extract
  mv /tmp/cmdline-tools-extract/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
  rm /tmp/cmdline-tools.zip
  rm -rf /tmp/cmdline-tools-extract
  echo "      Command-line tools installed."
else
  echo "      Command-line tools already present, skipping."
fi

# Env variables (idempotent)
PROFILE_FILE="$HOME/.bashrc"
if ! grep -q "ANDROID_HOME" "$PROFILE_FILE"; then
  echo "" >> "$PROFILE_FILE"
  echo "# Android SDK" >> "$PROFILE_FILE"
  echo "export ANDROID_HOME=$ANDROID_HOME" >> "$PROFILE_FILE"
  echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> "$PROFILE_FILE"
fi

export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"
export ANDROID_HOME="$ANDROID_HOME"

echo "      Accepting SDK licenses..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true

echo "      Installing SDK components (compileSdk=36, build-tools=36.0.0)..."
sdkmanager --quiet \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

echo "      Android SDK ready."

# ── 6. Repo local.properties ─────────────────────────
echo ""
echo "[6/6] Configuring repo..."

if [ -d "$REPO_DIR" ]; then
  LOCAL_PROPS="$REPO_DIR/local.properties"
  if [ ! -f "$LOCAL_PROPS" ] || ! grep -q "sdk.dir" "$LOCAL_PROPS"; then
    echo "sdk.dir=$ANDROID_HOME" >> "$LOCAL_PROPS"
    echo "      Created $LOCAL_PROPS"
  else
    echo "      local.properties already has sdk.dir, skipping."
  fi
else
  echo "      WARNING: Repo not found at $REPO_DIR"
  echo "      Run manually after cloning: echo 'sdk.dir=$ANDROID_HOME' >> $REPO_DIR/local.properties"
fi

# ── Done ─────────────────────────────────────────────
echo ""
echo "================================================="
echo "  Setup complete! Manual steps remaining:"
echo "================================================="
echo ""
echo "  1. Authenticate Claude CLI:"
echo "     claude"
echo "     (or: export ANTHROPIC_API_KEY=sk-ant-...)"
echo ""
echo "  2. Authenticate GitHub CLI:"
echo "     gh auth login"
echo ""
echo "  3. Copy google-services.json to the repo:"
echo "     # From your local machine:"
echo "     scp app/google-services.json node@34.118.7.102:$REPO_DIR/app/"
echo ""
echo "  4. Reload shell env:"
echo "     source ~/.bashrc"
echo ""
echo "  5. Smoke test the build:"
echo "     cd $REPO_DIR && ./gradlew assembleDebug"
echo ""
