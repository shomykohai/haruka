{
  description = "Haruka dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    frostix.url = "github:shomykohai/frostix";
  };

  outputs = {
    self,
    nixpkgs,
    ...
  } @ inputs: let
    system = "x86_64-linux";
    pkgs = nixpkgs.legacyPackages.${system};
    frostix = inputs.frostix.packages.${system};
  in {
    devShells.${system}.default = pkgs.mkShell {
      packages = [
        pkgs.jdk8
        pkgs.unzip
        pkgs.zip
        frostix.dex2jar
        frostix.dexpatcher
      ];
    };
  };
}
