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
#+setup_end+#

#+elab_begin+#
redirect #*dc_sub_log*# {
	set rvs [elaborate #*root_sub*# -architecture verilog -library DEFAULT]
}
if {$rvs == 0} {
	echo "elab_fail #*root_sub*#"
}
#+elab_end+#

#+setdelay_begin+#
redirect -append #*dc_sub_log*# {
	set rvs [set_min_delay -from [get_ports { #*from_sub*# }] -to [get_ports { #*to_sub*# }] #*time_min_sub*#]
}
if {$rvs == 0} {
	echo "setdelay_fail #*root_sub*#"
}
redirect -append #*dc_sub_log*# {
	set rvs [set_max_delay -from [get_ports { #*from_sub*# }] -to [get_ports { #*to_sub*# }] #*time_max_sub*#]
}
if {$rvs == 0} {
	echo "setdelay_fail #*root_sub*#"
}
#+setdelay_end+#

#+settouch_begin+#
redirect -append #*dc_sub_log*# {
	set rvs [set_dont_touch_network [get_ports { #*touch_sub*# }]]
}
if {$rvs == 0} {
	echo "settouch_fail #*root_sub*#"
}
#+settouch_end+#

#+compile_begin+#
redirect -append #*dc_sub_log*# {
	set rvs [compile]
}
if {$rvs == 0} {
	echo "compile_fail #*root_sub*#"
}
#+compile_end+#

#+final_begin+#
redirect -append #*dc_log*# {
	set rvs [elaborate #*root*# -architecture verilog -library DEFAULT]
}
if {$rvs == 0} {
	echo "elab_fail #*root*#"
	exit 1
}
redirect -append #*dc_log*# {
	set rvs [write -hierarchy -format verilog -output {#*outfile*#}]
}
if {$rvs == 0} {
	echo "write_fail #*root*#"
	exit 1
}
exit 2
#+final_end+#
