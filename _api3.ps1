$ErrorActionPreference = 'Continue'
$J = "C:\Users\wdsjx\Desktop\infinityminecraft-template-1.21.1\build\moddev\artifacts\neoforge-21.1.252-merged.jar"
Write-Output "===== SpriteSheetParticle ctor ====="
& javap -cp $J -p net.minecraft.client.particles.SpriteSheetParticle 2>&1 | Select-String 'SpriteSheetParticle\(', 'class '
Write-Output "===== TextureSheetParticle exists? ====="
& javap -cp $J -p net.minecraft.client.particles.TextureSheetParticle 2>&1 | Select-Object -First 2
Write-Output "===== ParticleProvider ====="
& javap -cp $J -p net.minecraft.client.particles.ParticleProvider 2>&1
Write-Output "===== SpriteSetAware ====="
& javap -cp $J -p net.minecraft.client.particles.SpriteSetAware 2>&1
Write-Output "===== ParticleEngine.SpriteParticleRegistration ====="
& javap -cp $J -p 'net.minecraft.client.particles.ParticleEngine$SpriteParticleRegistration' 2>&1
