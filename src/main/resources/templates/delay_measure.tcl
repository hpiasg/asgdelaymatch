#+setup_begin+#
sh rm -f default.svf
sh rm -rf dccompile
sh mkdir dccompile
define_design_lib WORK -path ./dccompile
redirect #*dc_log*# {
	set rvs [analyze -library WORK -format verilog {#*orig*#}]
}
if {$rvs == 0} {
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

#+measure_max_begin+#
redirect -append #*dc_sub_log*# {
	set rvs [report_timing -from { #*from_sub*# } -to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
	echo "report_timing_fail #*root_sub*# from: { #*from_sub*# } to: { #*to_sub*# }"
}
#+measure_max_end+#

#+measure_min_begin+#
redirect -append #*dc_sub_log*# {
	set rvs [report_timing -rise_from { #*from_sub*# } -to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
	echo "report_timing_fail #*root_sub*# from: { #*from_sub*# } to: { #*to_sub*# }"
}
#+measure_min_end+#

#+final_begin+#
exit 2
#+final_end+#
