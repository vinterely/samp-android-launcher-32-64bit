//
// Created by GLORY TEAM.
//

#include "TextureDatabaseEntry.h"
#include "../vendor/armhook/patch.h"

void TextureDatabaseEntry::InjectHooks() {
    //CHook::Redirect("_ZN20TextureDatabaseEntry12LoadInstanceERK23TextureListingContainer", &TextureDatabaseEntry__LoadInstance);
}
