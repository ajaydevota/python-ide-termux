import io
import os
import re
import sys
import ssl
import json
import zipfile
import traceback

IMAGE_EXT = ("png", "jpg", "jpeg", "bmp", "gif")
VIDEO_EXT = ("mp4", "webm", "avi")


# ---------- paths ----------

def _base_dir(outdir):
    return os.path.dirname(outdir)


def _site_dir(outdir):
    site = os.path.join(_base_dir(outdir), "site-packages")
    try:
        os.makedirs(site, exist_ok=True)
    except Exception:
        pass
    return site


def ensure_paths(outdir):
    site = _site_dir(outdir)
    if site not in sys.path:
        sys.path.insert(0, site)
    repos = os.path.join(_base_dir(outdir), "repos")
    try:
        if os.path.isdir(repos):
            for d in sorted(os.listdir(repos)):
                p = os.path.join(repos, d)
                if os.path.isdir(p) and p not in sys.path:
                    sys.path.append(p)
    except Exception:
        pass
    return site


# ---------- network helpers ----------

def _ssl_ctx():
    try:
        return ssl.create_default_context()
    except Exception:
        return None


def _download(url, timeout=90):
    import urllib.request
    req = urllib.request.Request(url, headers={"User-Agent": "PythonIDE"})
    with urllib.request.urlopen(req, timeout=timeout, context=_ssl_ctx()) as r:
        return r.read()


def check_module(name):
    try:
        import importlib.util
        return importlib.util.find_spec(name) is not None
    except Exception:
        return False


# ---------- runtime package install (pure-Python only) ----------

def pip_install(name, outdir):
    site = _site_dir(outdir)
    ensure_paths(outdir)
    name = (name or "").strip()
    if not name:
        return "ERROR: package name khali hai."
    if check_module(name):
        return "OK: '" + name + "' pehle se available hai."
    try:
        raw = _download("https://pypi.org/pypi/%s/json" % name)
        meta = json.loads(raw.decode("utf-8"))
    except Exception as e:
        return "ERROR: PyPI par '" + name + "' nahi mila (" + str(e) + ")."
    files = meta.get("urls", [])
    chosen = None
    for f in files:
        fn = f.get("filename", "")
        if fn.endswith(".whl") and ("py3-none-any" in fn or "py2.py3-none-any" in fn):
            chosen = f
            break
    if chosen is None:
        return ("ERROR: '" + name + "' ka pure-Python wheel nahi mila. "
                "Ye native package ho sakta hai (numpy/matplotlib jaise) - "
                "use APK build ke waqt pip block me jodna padta hai.")
    try:
        data = _download(chosen["url"])
        zipfile.ZipFile(io.BytesIO(data)).extractall(site)
    except Exception as e:
        return "ERROR: '" + name + "' install nahi hua (" + str(e) + ")."
    try:
        import importlib
        importlib.invalidate_caches()
    except Exception:
        pass
    if check_module(name):
        return "OK: '" + name + "' install ho gaya (" + chosen["filename"] + ") - ab import kar sakte hain."
    return "WARN: '" + name + "' extract hua par import nahi ho raha (dependency missing ho sakti hai)."


# ---------- git clone (GitHub archive download + extract) ----------

def git_clone(url, outdir):
    repos = os.path.join(_base_dir(outdir), "repos")
    try:
        os.makedirs(repos, exist_ok=True)
    except Exception:
        pass
    m = re.search(r"github\.com[/:]([^/\s]+)/([^/\s\.]+)", url or "")
    if not m:
        return "ERROR: GitHub URL samajh nahi aaya. Udaharan: https://github.com/psf/requests"
    owner, repo = m.group(1), m.group(2)
    if repo.endswith(".git"):
        repo = repo[:-4]
    data = None
    used = ""
    for branch in ("main", "master"):
        zurl = "https://github.com/%s/%s/archive/refs/heads/%s.zip" % (owner, repo, branch)
        try:
            data = _download(zurl, timeout=120)
            used = branch
            break
        except Exception:
            continue
    if data is None:
        return "ERROR: '" + owner + "/" + repo + "' download nahi hua (repo/branch galat, ya internet nahi)."
    dest = os.path.join(repos, repo)
    try:
        z = zipfile.ZipFile(io.BytesIO(data))
        names = z.namelist()
        top = names[0].split("/")[0] if names else ""
        for n in names:
            if top and not n.startswith(top + "/"):
                continue
            rel = n[len(top) + 1:] if top else n
            if not rel:
                continue
            target = os.path.join(dest, rel)
            if n.endswith("/"):
                os.makedirs(target, exist_ok=True)
            else:
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with open(target, "wb") as fh:
                    fh.write(z.read(n))
    except Exception as e:
        return "ERROR: extract nahi hua (" + str(e) + ")."
    ensure_paths(outdir)
    return "OK: " + owner + "/" + repo + " clone ho gaya (branch " + used + ") -> " + dest


# ---------- matplotlib hook ----------

def _setup_matplotlib(outdir):
    try:
        import matplotlib
        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        if not getattr(plt, "_pyide_patched", False):
            def _show(*args, **kwargs):
                try:
                    n = 1
                    while True:
                        p = os.path.join(outdir, "figure_%d.png" % n)
                        if not os.path.exists(p):
                            break
                        n += 1
                    plt.savefig(p, dpi=110, bbox_inches="tight")
                except Exception:
                    pass
            plt.show = _show
            plt._pyide_patched = True
    except Exception:
        pass


# ---------- run ----------

def run(code, outdir):
    try:
        os.makedirs(outdir, exist_ok=True)
    except Exception:
        pass

    try:
        for f in os.listdir(outdir):
            try:
                os.remove(os.path.join(outdir, f))
            except Exception:
                pass
    except Exception:
        pass

    ensure_paths(outdir)
    _setup_matplotlib(outdir)

    try:
        old_cwd = os.getcwd()
    except Exception:
        old_cwd = None
    try:
        os.chdir(outdir)
    except Exception:
        pass

    buf = io.StringIO()
    old_out, old_err = sys.stdout, sys.stderr
    sys.stdout = buf
    sys.stderr = buf
    try:
        exec(compile(code, "<main>", "exec"),
             {"__name__": "__main__", "OUTDIR": outdir})
    except SystemExit:
        pass
    except Exception:
        buf.write("\n" + traceback.format_exc())
    finally:
        sys.stdout = old_out
        sys.stderr = old_err
        if old_cwd is not None:
            try:
                os.chdir(old_cwd)
            except Exception:
                pass

    artifacts = []
    try:
        for f in sorted(os.listdir(outdir)):
            ext = f.rsplit(".", 1)[-1].lower() if "." in f else ""
            path = os.path.join(outdir, f)
            if ext in VIDEO_EXT:
                artifacts.append({"type": "video", "path": path})
            elif ext in IMAGE_EXT:
                artifacts.append({"type": "image", "path": path})
    except Exception:
        pass

    return json.dumps({"text": buf.getvalue(), "artifacts": artifacts})
