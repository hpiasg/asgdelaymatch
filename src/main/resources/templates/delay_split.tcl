#+setup_begin+#
sh rm -f default.svf
sh rm -rf dccompile
sh mkdir dccompile
define_design_lib WORK -path ./dccompile

lappend search_path #*search_path*#
set link_library { #*libraries*# }
set target_library { #*libraries*# }

redirect #*dc_log*# {
	set rvs [analyze -library WORK -format verilog {#*orig*#}]
}
if {$rvs == 0} {
	echo "analyse_fail #*root*#"
	exit 1
}

redirect #*dc_log*# {
	set rvs [elaborate #*root*# -architecture verilog -library DEFAULT]
}
if {$rvs == 0} {
	echo "elab_fail #*root*#"
}

#+setup_end+#

#+generate_begin+#
redirect #*dc_log*# {
	set rvs [write_sdf -significant_digits 10 #*root_sdf*#]
}
if {$rvs == 0} {
	echo "write_sdf_fail #*root_sdf*#"
}

#+generate_end+#

#+read_begin+#
redirect #*dc_log*# {
	set rvs [read_sdf #*root_sdf*#]
}
if {$rvs == 0} {
	echo "read_sdf_fail #*root_sdf*#"
}

#+read_end+#

#+split_begin+#
redirect -append #*dc_log*# {
	set rvs [write_sdf -instance #*inst_name*# -significant_digits 10 #*inst_sdf_file*#]
}
if {$rvs == 0} {
	echo "write_sdf_fail #*inst_name*#"
}

#+split_end+#

#+final_begin+#
exit 2
#+final_end+#
