#pragma once

#include <cstdint>

#define OBJECT_MATERIAL_SIZE_32x32  10
#define OBJECT_MATERIAL_SIZE_64x32  20
#define OBJECT_MATERIAL_SIZE_64x64  30
#define OBJECT_MATERIAL_SIZE_128x32  40
#define OBJECT_MATERIAL_SIZE_128x64  50
#define OBJECT_MATERIAL_SIZE_128x128  60
#define OBJECT_MATERIAL_SIZE_256x32  70
#define OBJECT_MATERIAL_SIZE_256x64  80
#define OBJECT_MATERIAL_SIZE_256x128  90
#define OBJECT_MATERIAL_SIZE_256x256  100
#define OBJECT_MATERIAL_SIZE_512x64  110
#define OBJECT_MATERIAL_SIZE_512x128  120
#define OBJECT_MATERIAL_SIZE_512x256  130
#define OBJECT_MATERIAL_SIZE_512x512  140

struct RwTexture;

class MaterialTextGenerator
{
public:
	MaterialTextGenerator();
	~MaterialTextGenerator();

	RwTexture* Generate(const char* text, int size, int font_size, bool bold, uint32_t font_color, uint32_t background_color, int alignment);

private:
	void GetMaterialSize(int size, int* x, int* y);
};
