{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  buildInputs = with pkgs; [
    openjdk21
    maven

    # X11 libraries
    xorg.libX11
    xorg.libXext
    xorg.libXrender
    xorg.libXtst
    xorg.libXi
    xorg.libXrandr
    xorg.libXcursor
    xorg.libXinerama
    xorg.libXxf86vm
    xorg.libXft

    # GTK and related libraries (crucial for JavaFX)
    gtk3
    gdk-pixbuf
    glib
    cairo
    pango
    atk

    # Graphics libraries
    libGL
    libGLU
    mesa

    # Font libraries
    freetype
    fontconfig

    # Additional libraries that JavaFX might need
    alsa-lib
    libpulseaudio
    libudev0-shim
  ];

  shellHook = ''
    export LD_LIBRARY_PATH=${pkgs.lib.makeLibraryPath [
      # X11 libraries
      pkgs.xorg.libX11
      pkgs.xorg.libXext
      pkgs.xorg.libXrender
      pkgs.xorg.libXtst
      pkgs.xorg.libXi
      pkgs.xorg.libXrandr
      pkgs.xorg.libXcursor
      pkgs.xorg.libXinerama
      pkgs.xorg.libXxf86vm
      pkgs.xorg.libXft

      # GTK and related
      pkgs.gtk3
      pkgs.gdk-pixbuf
      pkgs.glib
      pkgs.cairo
      pkgs.pango
      pkgs.atk

      # Graphics
      pkgs.libGL
      pkgs.libGLU
      pkgs.mesa

      # Fonts
      pkgs.freetype
      pkgs.fontconfig

      # Audio and other
      pkgs.alsa-lib
      pkgs.libpulseaudio
      pkgs.libudev0-shim
    ]}:$LD_LIBRARY_PATH

    # Set GTK theme path
    export GTK_PATH=${pkgs.gtk3}/lib/gtk-3.0
    export GTK_DATA_PREFIX=${pkgs.gtk3}

    # Set GDK pixbuf loaders
    export GDK_PIXBUF_MODULE_FILE=${pkgs.gdk-pixbuf}/lib/gdk-pixbuf-2.0/2.10.0/loaders.cache

    echo "JavaFX development environment loaded!"
    echo "LD_LIBRARY_PATH includes GTK3 and X11 libraries"
    echo "You can now run your JavaFX applications."
  '';
}