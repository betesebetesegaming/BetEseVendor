/* BetEse Vendor — download page
 * Fetches releases from the public GitHub repo and renders download buttons.
 * No token needed because the repo is public.
 */
(function () {
  "use strict";

  var REPO = "betesebetesegaming/BetEseVendor";
  var API = "https://api.github.com/repos/" + REPO + "/releases";
  var RELEASES_PAGE = "https://github.com/" + REPO + "/releases";

  var latestEl = document.getElementById("latest");
  var listEl = document.getElementById("releases-list");
  var fineprintEl = document.getElementById("fineprint");
  var yearEl = document.getElementById("year");
  if (yearEl) yearEl.textContent = new Date().getFullYear();

  // ---------- helpers ----------
  function esc(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }

  function fmtSize(bytes) {
    if (!bytes && bytes !== 0) return "";
    var mb = bytes / (1024 * 1024);
    if (mb >= 1) return mb.toFixed(1) + " MB";
    return Math.max(1, Math.round(bytes / 1024)) + " KB";
  }

  function fmtDate(iso) {
    if (!iso) return "";
    try {
      return new Date(iso).toLocaleDateString(undefined, {
        year: "numeric", month: "short", day: "numeric"
      });
    } catch (e) { return ""; }
  }

  // Pick the .apk asset from a release (fallback: first asset).
  function apkAsset(release) {
    var assets = release.assets || [];
    for (var i = 0; i < assets.length; i++) {
      if (/\.apk$/i.test(assets[i].name)) return assets[i];
    }
    return assets.length ? assets[0] : null;
  }

  function versionLabel(release) {
    return release.name || release.tag_name || "Release";
  }

  // ---------- render: latest ----------
  function renderLatest(release) {
    var asset = apkAsset(release);
    var ver = esc(versionLabel(release));
    var date = fmtDate(release.published_at || release.created_at);
    var size = asset ? fmtSize(asset.size) : "";
    var downloads = asset && asset.download_count != null ? asset.download_count : null;

    var metaParts = [];
    if (date) metaParts.push('<span>📅 ' + esc(date) + "</span>");
    if (size) metaParts.push('<span>💾 ' + esc(size) + "</span>");
    if (downloads != null) metaParts.push('<span>⬇️ ' + downloads.toLocaleString() + " downloads</span>");

    var notes = (release.body || "").trim();
    var notesHtml = notes
      ? '<details class="release-notes"><summary>What\'s new</summary>' +
        '<div class="notes-body">' + esc(notes) + "</div></details>"
      : "";

    var btn = asset
      ? '<a class="btn-download" href="' + esc(asset.browser_download_url) + '" download>' +
        '<span class="arrow">⬇️</span> Download Latest APK</a>'
      : '<a class="btn-download" href="' + esc(release.html_url) + '" target="_blank" rel="noopener">' +
        "View release on GitHub</a>";

    latestEl.innerHTML =
      '<div class="latest-top">' +
        '<span class="latest-version">' + ver + "</span>" +
        '<span class="badge-latest">Latest</span>' +
      "</div>" +
      '<div class="latest-meta">' + metaParts.join("") + "</div>" +
      btn +
      notesHtml;

    if (fineprintEl) fineprintEl.hidden = false;
  }

  // ---------- render: list ----------
  function renderList(releases) {
    var rows = releases.map(function (r, idx) {
      var asset = apkAsset(r);
      var ver = esc(versionLabel(r));
      var date = fmtDate(r.published_at || r.created_at);
      var size = asset ? fmtSize(asset.size) : "";
      var dls = asset && asset.download_count != null ? asset.download_count : null;

      var subParts = [];
      if (date) subParts.push(esc(date));
      if (size) subParts.push(esc(size));
      if (dls != null) subParts.push(dls.toLocaleString() + " downloads");

      var latestTag = idx === 0 ? '<span class="badge-latest">Latest</span>' : "";

      var btn = asset
        ? '<a class="btn-row" href="' + esc(asset.browser_download_url) + '" download>⬇️ Download APK</a>'
        : '<a class="btn-row" href="' + esc(r.html_url) + '" target="_blank" rel="noopener">View on GitHub</a>';

      return (
        '<div class="release-row">' +
          '<div class="release-info">' +
            '<div class="release-title">' + ver + " " + latestTag + "</div>" +
            '<div class="release-sub">' + subParts.join(" · ") + "</div>" +
          "</div>" +
          btn +
        "</div>"
      );
    });

    listEl.innerHTML = rows.join("");
  }

  // ---------- empty / error states ----------
  function showNoReleases() {
    var html =
      '<div class="state-box">' +
        "<strong>No releases published yet</strong>" +
        "<p>Once the first APK is uploaded to GitHub Releases, it will appear here automatically.</p>" +
        '<a href="' + RELEASES_PAGE + '" target="_blank" rel="noopener">Open Releases on GitHub ↗</a>' +
      "</div>";
    latestEl.innerHTML = html;
    listEl.innerHTML = "";
  }

  function showError(msg) {
    var html =
      '<div class="state-box">' +
        "<strong>Couldn’t load releases</strong>" +
        "<p>" + esc(msg) + "</p>" +
        '<a href="' + RELEASES_PAGE + '" target="_blank" rel="noopener">Download directly from GitHub ↗</a>' +
      "</div>";
    latestEl.innerHTML = html;
    listEl.innerHTML = "";
  }

  // ---------- fetch ----------
  fetch(API, { headers: { Accept: "application/vnd.github+json" } })
    .then(function (res) {
      if (res.status === 403) {
        throw new Error("GitHub rate limit reached. Please try again in a little while, or use the GitHub link below.");
      }
      if (!res.ok) throw new Error("GitHub returned status " + res.status + ".");
      return res.json();
    })
    .then(function (data) {
      var releases = (data || []).filter(function (r) { return !r.draft; });
      if (!releases.length) { showNoReleases(); return; }
      renderLatest(releases[0]);
      renderList(releases);
    })
    .catch(function (err) {
      showError(err && err.message ? err.message : "Network error.");
    });
})();
