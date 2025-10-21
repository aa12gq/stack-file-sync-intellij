$ErrorActionPreference = 'Stop'

$packageName = 'stack-sync'
$url64 = 'https://github.com/stackfilesync/stack-sync-cli/releases/download/v1.1.0/stack-sync-windows-amd64.exe'
$checksum64 = 'YOUR_CHECKSUM_HERE'
$checksumType64 = 'sha256'

$toolsDir = "$(Split-Path -parent $MyInvocation.MyCommand.Definition)"
$filePath = Join-Path $toolsDir "stack-sync.exe"

Get-ChocolateyWebFile -PackageName $packageName -FileFullPath $filePath -Url64bit $url64 -Checksum64 $checksum64 -ChecksumType64 $checksumType64

# Create shim
Install-BinFile -Name "stack-sync" -Path $filePath
