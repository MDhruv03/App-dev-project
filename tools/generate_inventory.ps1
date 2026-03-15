$root = (Get-Location).Path
$outFile = Join-Path $root "PROJECT_FILE_INVENTORY.md"

function Get-RelPath([string]$fullPath) {
    return $fullPath.Substring($root.Length + 1).Replace("\\", "/")
}

function Get-DirRel([string]$dirPath) {
    if ([string]::IsNullOrEmpty($dirPath)) {
        return "."
    }

    $rel = $dirPath.Substring($root.Length).TrimStart("\\").Replace("\\", "/")
    if ([string]::IsNullOrEmpty($rel)) {
        return "."
    }
    return $rel
}

function Get-JavaDescription([string]$filePath, [string]$relPath) {
    if ($relPath -match "(^|/)build/" -or $relPath -match "^\\.gradle/") {
        return "Generated Java-related artifact produced by build tooling for compilation/indexing."
    }

    $name = [System.IO.Path]::GetFileNameWithoutExtension($filePath)
    $text = ""
    try {
        $text = (Get-Content -Path $filePath -TotalCount 220 -ErrorAction Stop | Out-String)
    } catch {
        $text = ""
    }

    $kind = "type"
    $decl = $name
    $m = [regex]::Match($text, "public\s+(?:abstract\s+)?(class|interface|enum)\s+([A-Za-z_][A-Za-z0-9_]*)")
    if ($m.Success) {
        $kind = $m.Groups[1].Value
        $decl = $m.Groups[2].Value
    }

    if ($text -match "@Entity") {
        return "Room entity $decl that maps a database table and its persisted fields."
    }
    if ($text -match "@Dao" -or $name -match "Dao$") {
        return "Room DAO $decl containing SQL query and persistence methods for its entity/domain models."
    }
    if ($name -match "Fragment$") {
        return "UI Fragment $decl implementing a screen flow, view bindings, and user interactions."
    }
    if ($name -match "ViewModel$") {
        return "ViewModel $decl managing UI state, LiveData, and repository coordination for its feature."
    }
    if ($name -match "Repository$") {
        return "Repository $decl handling data access, sync strategy, and local/remote source coordination."
    }
    if ($name -match "Adapter$") {
        return "Recycler/List adapter $decl that binds domain items to UI rows/cards."
    }
    if ($name -match "Activity$") {
        return "Android Activity $decl implementing a full-screen user flow and lifecycle handlers."
    }
    if ($text -match "implements\s+ApiService") {
        return "API service implementation $decl that fulfills ApiService endpoints and response mapping."
    }
    if ($name -match "ApiService$" -or $name -match "ApiCallback$") {
        return "Networking contract/utility $decl defining API methods or callback interfaces."
    }
    if ($name -match "Helper$" -or $name -match "Utils?$" -or $name -match "Manager$") {
        return "Utility/helper class $decl with shared feature support logic."
    }
    if ($name -eq "MainActivity") {
        return "MainActivity hosting bottom navigation and switching between feature fragments."
    }
    if ($name -eq "OpportunityHubApplication") {
        return "Application class OpportunityHubApplication initializing app services, diagnostics, database, and sync scheduling."
    }

    return "Java $kind $decl containing feature/domain logic used by the Android app."
}

function Get-Description([System.IO.FileInfo]$file) {
    $rel = Get-RelPath $file.FullName
    $ext = $file.Extension.ToLowerInvariant()
    $name = $file.Name

    if ($rel -match "^\\.gradle/" -or $rel -match "(^|/)build/") {
        switch ($ext) {
            ".xml" { return "Generated build metadata XML used by Android/Gradle tooling for variants, manifests, or resources." }
            ".bin" { return "Binary Gradle cache/state artifact used for incremental builds and task history." }
            ".lock" { return "Gradle lock file guarding concurrent access to caches or task state." }
            ".json" { return "Generated build report/metadata in JSON for tooling diagnostics or task outputs." }
            ".txt" { return "Generated textual report/log artifact produced during build tasks." }
            ".jar" { return "Generated or cached JAR dependency/artifact used during compilation or packaging." }
            ".class" { return "Compiled Java bytecode class generated from source during build." }
            ".dex" { return "Compiled Dalvik/ART bytecode artifact for Android runtime packaging." }
            ".apk" { return "Generated Android application package artifact from build output." }
            default { return "Generated or cached build artifact used by Gradle/Android tooling." }
        }
    }

    switch ($ext) {
        ".java" { return Get-JavaDescription $file.FullName $rel }
        ".xml" {
            if ($rel -match "AndroidManifest\\.xml$") {
                return "Android manifest declaring app package, components, permissions, and app-level configuration."
            }
            if ($rel -match "^app/src/.*/res/layout/") {
                return "Layout XML defining UI structure for screen/component $name."
            }
            if ($rel -match "^app/src/.*/res/menu/") {
                return "Menu XML defining app navigation/actions in $name."
            }
            if ($rel -match "^app/src/.*/res/values/") {
                return "Resource values XML (strings/colors/styles/dimens/attrs) declared in $name."
            }
            if ($rel -match "^app/src/.*/res/drawable/") {
                return "Drawable resource XML for icons/shapes/selectors defined in $name."
            }
            if ($rel -match "^app/src/.*/res/mipmap/") {
                return "Launcher icon or density-specific image XML resource $name."
            }
            if ($rel -match "^\\.idea/") {
                return "IDE project configuration XML used by Android Studio/IntelliJ."
            }
            return "XML configuration/resource file $name used by Android app or tooling."
        }
        ".gradle" { return "Gradle build script defining plugins, dependencies, and build configuration for this module/project." }
        ".properties" { return "Properties configuration file containing build/tool/environment settings." }
        ".toml" { return "Version catalog TOML defining centralized dependency and plugin versions." }
        ".md" { return "Markdown documentation file containing project guidance, status, or instructions." }
        ".txt" { return "Plain-text project note, report, or roadmap content." }
        ".bat" { return "Windows batch script for project automation tasks such as setup/build/log collection." }
        ".ps1" { return "PowerShell automation script for project tooling and diagnostics." }
        ".yml" { return "YAML workflow/configuration file used by CI or project automation." }
        ".yaml" { return "YAML workflow/configuration file used by CI or project automation." }
        ".gitignore" { return "Git ignore rules file specifying untracked patterns for version control." }
        ".iml" { return "IntelliJ module descriptor defining IDE module metadata." }
        ".keystore" { return "Keystore file for signing Android builds (sensitive credential material)." }
        ".jks" { return "Java keystore for signing or certificate storage." }
        ".png" { return "Image asset used by app UI or launcher resources." }
        ".webp" { return "WebP image asset used by Android drawable/mipmap resources." }
        ".svg" { return "Vector image asset used for icons or diagrams." }
        ".log" { return "Log file capturing runtime/build diagnostic output." }
        ".db" { return "Database file containing persisted local application data." }
        default { return "Project file or artifact used by source code, tooling, or documentation." }
    }
}

$files = Get-ChildItem -Recurse -File | Where-Object { $_.FullName -notmatch "\\\\.git\\\\" } | Sort-Object FullName

$sw = [System.IO.StreamWriter]::new($outFile, $false, [System.Text.Encoding]::UTF8)
$sw.WriteLine("# Project File Inventory")
$sw.WriteLine("")
$sw.WriteLine("Total files: $($files.Count)")
$sw.WriteLine("")

$currentDir = $null
foreach ($file in $files) {
    $dirRel = Get-DirRel $file.DirectoryName
    if ($dirRel -ne $currentDir) {
        if ($null -ne $currentDir) {
            $sw.WriteLine("")
        }
        $sw.WriteLine("## $dirRel")
        $currentDir = $dirRel
    }

    $rel = Get-RelPath $file.FullName
    $desc = Get-Description $file
    $sw.WriteLine(("- {0}: {1}" -f $rel, $desc))
}

$sw.Close()
Write-Output "Wrote $($files.Count) entries to $outFile"