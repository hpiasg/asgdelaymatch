ASGdelaymatch
-------------

ASGdelaymatch inserts matched delays into a (resynthesised) bundled-data Balsa circuit.

### Installation ###

Download and unpack the appropriate package for your operating system (currently only UNIX-based systems are supported). You don't have to install anything or make changes to environment variables. To run it you will need a Java runtime environment (JRE) v1.7 (or later).

### Configuration ###

##### Main configuration file #####

The default configuration file is `ASGdelaymatch_DIR/config/delaymatchconfig.xml`. You can specify another config file with the `-cfg <file>` option of ASGresyn.

The `<workdir>` tag specifies a path where ASGresyn stores all temporary files during operation. The default value is empty (and therefore a default operating system directory is used). You can override these settings with `-w <dir>` option of ASGresyn.

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
If your Design Compiler executable differs from `dc_shell`, please adjust this in `ASGdelaymatch_DIR/templates/delay_measure.sh` and `ASGdelaymatch_DIR/templates/delay_match.sh`.

##### Profile file #####

Within the profile file you can specify how components should be delay matched. In this bundle a profile for resynthesis (`ASGdelaymatch_DIR/config/resynprofile.xml`) is already included.

You can find the XML schema [here](src/main/resources/schema/profile_config.xsd).

### Usage ###

For the following example commands it is assumed that your current working directory is the ASGdelaymatch main directory. If you want run ASGdelaymatch from another directory you have to add the path to the ASGdelaymatch main directory in front of the following commands (or you could add the `bin/` directory to your `PATH` variable).

##### List of supported arguments #####

To see a list of supported command line arguments execute

    bin/ASGdelaymatch
    
#### Simple delay matching ####

For simple delay matching execute

	bin/ASGdelaymatch -p config/resynprofile.xml -out outfile.v infile.v

The `-p` option is required expects a profile configuration file as described above.

#### Contol-aware delay matching ####

For control-aware delay matching execute

	bin/ASGdelaymatch -p config/resynprofile.xml -out outfile.v -past stg.g infile.v

The option `-past` expects the Balsa-STG which was used for the implementation of infile.v

### Build instructions ###

To build ASGdelaymatch, Apache Maven v3 (or later) and the Java Development Kit (JDK) v1.7 (or later) are required.

1. Build [ASGcommon](https://github.com/hpiasg/asgcommon)
2. Execute `mvn clean install -DskipTests`