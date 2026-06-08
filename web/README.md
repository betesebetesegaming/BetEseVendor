# BetEse Vendor — Download Site

A static landing page that lets vendors download the **BetEse Vendor** Android app.
It lists the **latest release** plus **every previous release**, pulling them live from
GitHub Releases (no backend, no secrets — the repo is public).

Live at: **https://vendor.betesepmu.com**

---

## How it works

```
vendor.betesepmu.com (Vercel)  ──fetches──▶  GitHub Releases API
        │                                     /repos/betesebetesegaming/BetEseVendor/releases
        └── serves web/index.html (static)
```

- `index.html` / `styles.css` / `app.js` — the whole site (vanilla, no build step).
- `app.js` calls the GitHub API and renders the download buttons automatically.
- **You never edit code to publish a new version.** Just create a GitHub Release with the APK.

---

## 📦 Publishing a new app version (do this for every update)

1. Build your signed APK in Android Studio
   (`Build → Generate Signed App Bundle / APK → APK`), e.g. `betese-vendor-1.1.apk`.
2. Go to **https://github.com/betesebetesegaming/BetEseVendor/releases/new**
3. Set:
   - **Tag:** `v1.1` (use your version number)
   - **Release title:** `BetEse Vendor 1.1`
   - **Description:** what changed (this shows under “What’s new” on the site)
4. **Drag the `.apk` file into the “Attach binaries” box.**
5. Click **Publish release**.

That’s it. Within seconds the site shows the new version as **Latest**, and the old
one drops into the “All releases” list — no redeploy needed.

> Tip: keep the file name ending in `.apk`. The site auto-detects the `.apk` asset.

---

## Local preview

```bash
cd web
python -m http.server 8080
# open http://localhost:8080
```

(With no releases yet, the page correctly shows “No releases published yet.”)

---

## Deploying (Vercel)

This folder is the site root. In the Vercel project settings set
**Root Directory = `web`**. No build command, no framework — it’s plain static files.
Custom domain: `vendor.betesepmu.com`.
