# Strontium

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.

## OpenGL 4.6

Strontium requests an OpenGL 4.6 core profile before Minecraft creates its GLFW
window. This is enabled by default and requires a driver that supports OpenGL
4.6. To disable the request for troubleshooting, start Minecraft with:

```text
-Dstrontium.opengl46=false
```
