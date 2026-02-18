# 1.4.5
Fix light being removed from the new location instead of the old location.

# 1.4.4
Fix player dying exception

# 1.4.3
Folia fix: run find best block logic in region thread.

# 1.4.2
Fix light location being compare between block & player eye wich was never the same making the light flicker.

# 1.3.4
Update to 1.21.11 by using co.aikar:acf-paper instead of the custom NMS commands code from github.xCykrix:spigotdevkit.
Reenable light toggle.
Remove light-culling-distance config option as we now use real light that can be seen from any distance.