# statusline.ps1 - 读取 Claude Code 通过 stdin 传入的上下文信息
try {
    $json = $input | ConvertFrom-Json
    if ($json -and $json.context_window) {
        $dir = Split-Path $json.workspace.current_dir -Leaf
        $model = $json.model.display_name
        $pct = [math]::Round($json.context_window.remaining_percentage)
        Write-Output ("📂 $dir | 🧠 $model | 💾 ${pct}%")
    } else {
        throw
    }
} catch {
    # 无 stdin 输入时降级显示基本信息
    $d = Split-Path (Get-Location) -Leaf
    $m = $env:ANTHROPIC_MODEL
    Write-Output ("📂 $d | 🧠 $m")
}
