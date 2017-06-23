ASGdelaymatch
-------------

ASGdelaymatch inserts matched delays into a (resynthesised) bundled-data Balsa circuit.

### Installation ###

Download and unpack the appropriate package for your operating system. You don't have to install anything or make changes to environment variables. To run it you will need a Java runtime environment (JRE) v1.7 (or later).

### Configuration ###

//TODO: Tech?, ConfigGen, TechMngr

##### Main configuration file #####

The default configuration file is `ASGdelaymatch_DIR/config/delaymatchconfig.xml`. You can specify another config file with the `-cfg <file>` option of ASGdelaymatch.

The `<workdir>` tag specifies a path where ASGdelaymtach stores all temporary files during operation. The default value is empty (and therefore a default operating system directory is used). You can override these settings with `-w <dir>` option of ASGdelaymatch.

Within the `<tools>` tag, the Synopsys Design Compiler's location can be configured. The Design Compiler is assumed to located on an external server:
```xml
<tools>
	<designCompilerCmd>
		<hostname></hostname>
		<username></username>
		<password></password>
		<workingdir></workingdir>
	</designCompilerCmd>
</tools>
```
If your Design Compiler executable differs from `dc_shell`, please adjust this in `ASGdelaymatch_DIR/templates/delaymatch_dc.sh`.

##### Profile file #####

Within the profile file you can specify how components should be delay matched. In this bundle profiles for resynthesis (`ASGdelaymatch_DIR/config/resynprofile.xml`) and plain-Balsa (`ASGdelaymatch_DIR/config/balsaprofile.xml`) are already included.

You can find the XML schema [here](src/main/resources/schema/profile_config.xsd).

### Usage ###

For the following example commands it is assumed that your current working directory is the ASGdelaymatch main directory. If you want run ASGdelaymatch from another directory you have to add the path to the ASGdelaymatch main directory in front of the following commands (or you could add the `bin/` directory to your `PATH` variable).

##### Runner #####

To run a graphical tool featuring input masks for all important command line arguments execute

    bin/ASGdelaymatch_run

##### List of supported arguments #####

To see a list of supported command line arguments execute

    bin/ASGdelaymatch
    
##### Simple delay matching #####

For simple delay matching execute

	bin/ASGdelaymatch -p config/resynprofile.xml -out outfile.v infile.v

The `-p` option is required expects a profile configuration file as described above.

##### Contol-aware delay matching (Resynthesis-only) #####

For control-aware delay matching execute

	bin/ASGdelaymatch -p config/resynprofile.xml -out outfile.v -past stg.g infile.v

The option `-past` expects the Balsa-STG which was used for the implementation of `infile.v`. There is also the option `-future` (without STG-file), however it has not that huge impact to performance of the circuit as the `-past` one has (Using both at the same time is possible, but the resulting timing is not guaranteed to be proper).

##### SDC Input/Output #####

//TODO

##### SDF Input/Output #####

//TODO


##### Verify #####

//TODO

### Build instructions ###

To build ASGdelaymatch, Apache Maven v3 (or later) and the Java Development Kit (JDK) v1.7 (or later) are required.

1. Build [ASGcommon](https://github.com/hpiasg/asgcommon)
2. Execute `mvn clean install -DskipTests`