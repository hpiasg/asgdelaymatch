ASGdelaymatch
-------------

ASGdelaymatch inserts matched delays into a (resynthesised) bundled-data Balsa circuit.

### Installation ###

Download and unpack the appropriate package for your operating system. You don't have to install anything or make changes to environment variables. To run it you will need a Java runtime environment (JRE) v1.7 (or later).

### Configuration ###

#### Main configuration file ####

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

You can generate a configuration file with [ASGconfigGen](https://github.com/hpiasg/asgconfiggen).

#### Profile file ####

Within the profile file you can specify how components should be delay matched. In this bundle profiles for resynthesis (`ASGdelaymatch_DIR/config/resynprofile.xml`) and plain-Balsa (`ASGdelaymatch_DIR/config/balsaprofile.xml`) are already included.

You can find the XML schema [here](src/main/resources/schema/profile_config.xsd).

### Usage ###

For the following example commands it is assumed that your current working directory is the ASGdelaymatch main directory. If you want run ASGdelaymatch from another directory you have to add the path to the ASGdelaymatch main directory in front of the following commands (or you could add the `bin/` directory to your `PATH` variable).

#### Runner ####

To run a graphical tool featuring input masks for all important command line arguments execute

    bin/ASGdelaymatch_run

#### List of supported arguments ####

To see a list of supported command line arguments execute

    bin/ASGdelaymatch
    
#### Simple delay matching ####

For simple delay matching execute

	bin/ASGdelaymatch -lib tech/techname.xml -p config/resynprofile.xml -out outfile.v infile.v

The `-p` option is required expects a profile configuration file as described above.

The `-lib` option expects a technology library file in the ASGtech XML format. You can create and install such a file with [ASGtechMngr](https://github.com/hpiasg/asgtechmngr).

#### Contol-aware delay matching (Resynthesis-only) ####

For control-aware delay matching execute

	bin/ASGdelaymatch -lib tech/techname.xml -p config/resynprofile.xml -out outfile.v -past stg.g infile.v

The option `-past` expects the Balsa-STG which was used for the implementation of `infile.v`. There is also the option `-future` (without STG-file), however it has not that huge impact to performance of the circuit as the `-past` one has (Using both at the same time is possible, but the resulting timing is not guaranteed to be proper).

#### SDC Input/Output ####

With the option `-sdcIn` you can specify an SDC file which will be used in every remote operation of ASGdelaymatch. The option `-sdcOut` generates an SDC file with the constraints implementing the matched delays (the file also includes the constraints from the `-sdcIn` file, if present).

#### SDF Input/Output ####

With the option `-sdfIn` you can specify an SDF file which will be used in the measurement step. Using this option, ASGdelaymtach is restricted to do only one iteration of delay matching, because after that the SDF file is not valid any more and ASGdelaymtach is not able to create a new one (because it doesn't know how the `-sdfIn` file was created in the first place, e.g. with layout). Thus, the user has to implement the iterative approach using his/her SDF creation process and ASGdelaymatch alternately. To improve this process there are also the options `-valIn` and `-valOut` to retain some data. For example:

	createVerilog verilog_1.v
	createSdf sdf_1.sdf
	bin/ASGdelaymatch -lib tech/techname.xml -p config/resynprofile.xml -sdfIn sdf_1.sdf -out verilog_2.v -valOut val_2.xml verilog_1.v
	createSdf sdf_2.sdf
	bin/ASGdelaymatch -lib tech/techname.xml -p config/resynprofile.xml -sdfIn sdf_2.sdf -valIn val_2.xml -out verilog_3.v -valOut val_3.xml verilog_2.v
	[...]

The option `-sdfOut` exports the last (temporary created) SDF file which was used during delay matching.

#### Verify ####

With the option `-verifyOnly` ASGdelaymatch only checks if the matched delay are met and will not modify the netlist.

#### Exid codes ####

ASGdelaymatch creates the following exit codes:
* 0: No timing violation(s)
* 1: Timing violation(s) detected
* 2: No statement (e.g. while using the `-sdfIn` option, ASGdelaymatch sets new matched delays but can't check them, because the SDF file is not valid any more)
* -1 or 255: Runtime Error

### Build instructions ###

To build ASGdelaymatch, Apache Maven v3 (or later) and the Java Development Kit (JDK) v1.7 (or later) are required.

1. Build [ASGcommon](https://github.com/hpiasg/asgcommon)
2. Execute `mvn clean install -DskipTests`