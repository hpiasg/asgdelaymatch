#+dc_tcl_setup_begin+#
sh rm -f default.svf
sh rm -rf dccompile
sh mkdir dccompile
define_design_lib WORK -path ./dccompile

lappend search_path #*dc_tcl_search_path*#
set link_library { #*dc_tcl_libraries*# }
set target_library { #*dc_tcl_libraries*# }
#+dc_tcl_setup_end+#

#+dc_tcl_finish_begin+#
exit 0
#+dc_tcl_finish_end+#

#+dc_tcl_analyze_begin+#
set rvs [analyze -library WORK -format verilog {#*dc_tcl_vin*#}]
if {$rvs == 0} {
	exit #*dc_tcl_exitcode*#
}
#+dc_tcl_analyze_end+#

#+dc_tcl_elab_begin+#
set rvs [elaborate #*dc_tcl_module*# -architecture verilog -library DEFAULT]
if {$rvs == 0} {
	exit #*dc_tcl_exitcode*#
}
#+dc_tcl_elab_end+#

#+dc_tcl_write_sdf_begin+#
write_sdf -significant_digits 10 #*dc_tcl_sdfout*#
#+dc_tcl_write_sdf_end+#

#+dc_tcl_read_sdf_begin+#
set rvs [read_sdf #*dc_tcl_sdffile*#]
if {$rvs == 0} {
	exit #*dc_tcl_exitcode*#
}
#+dc_tcl_read_sdf_end+#

#+dc_tcl_write_sdf_split_begin+#
write_sdf -instance #*dc_tcl_sdfinstname*# -significant_digits 10 #*dc_tcl_sdfout*#
if {$rvs == 0} {
	exit #*dc_tcl_exitcode*#
}
#+dc_tcl_write_sdf_split_end+#

#+dc_tcl_elab_sub_begin+#
redirect #*dc_tcl_sub_log*# {
	set rvs [elaborate #*dc_tcl_sub_module*# -architecture verilog -library DEFAULT]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
#+dc_tcl_elab_sub__end+#

#+dc_tcl_read_sdf_sub_begin+#
redirect #*dc_tcl_sub_log*# {
	set rvs [read_sdf #*dc_tcl_sub_sdffile*#]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
#+dc_tcl_read_sdf_sub_end+#

#+dc_tcl_setdelay_sub_begin+#
redirect -append #*dc_tcl_sub_log*# {
	set rvs [set_min_delay -from [get_ports { #*dc_tcl_sub_from*# }] -to [get_ports { #*dc_tcl_sub_to*# }] #*dc_tcl_sub_time_min*#]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
redirect -append #*dc_tcl_sub_log*# {
	set rvs [set_max_delay -from [get_ports { #*dc_tcl_sub_from*# }] -to [get_ports { #*dc_tcl_sub_to*# }] #*dc_tcl_sub_time_max*#]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
#+dc_tcl_setdelay_sub_end+#

#+dc_tcl_settouch_sub_begin+#
redirect -append #*dc_tcl_sub_log*# {
	set rvs [set_dont_touch_network [get_ports { #*dc_tcl_sub_donttouch*# }]]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
#+dc_tcl_settouch_sub_end+#

#+dc_tcl_compile_sub_begin+#
redirect -append #*dc_tcl_sub_log*# {
	set rvs [compile]
}
if {$rvs == 0} {
	echo #*dc_tcl_exitcode*#
}
#+dc_tcl_compile_sub_end+#


#+dc_tcl_measuremin_rise_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_rise_rise_end+#

#+dc_tcl_measuremin_rise_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_rise_fall_end+#

#+dc_tcl_measuremin_rise_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_rise_both_end+#

#+dc_tcl_measuremin_fall_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_fall_rise_end+#

#+dc_tcl_measuremin_fall_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_fall_fall_end+#

#+dc_tcl_measuremin_fall_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_fall_both_end+#

#+dc_tcl_measuremin_both_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_both_rise_end+#

#+dc_tcl_measuremin_both_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_both_fall_end+#

#+dc_tcl_measuremin_both_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremin_both_both_end+#

#+dc_tcl_measuremax_rise_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_rise_rise_end+#

#+dc_tcl_measuremax_rise_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_rise_fall_end+#

#+dc_tcl_measuremax_rise_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -rise_from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_rise_both_end+#

#+dc_tcl_measuremax_fall_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_fall_rise_end+#

#+dc_tcl_measuremax_fall_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_fall_fall_end+#

#+dc_tcl_measuremax_fall_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -fall_from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_fall_both_end+#

#+dc_tcl_measuremax_both_rise_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -rise_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_both_rise_end+#

#+dc_tcl_measuremax_both_fall_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -fall_to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_both_fall_end+#

#+dc_tcl_measuremax_both_both_begin+#
redirect -append #*dc_tcl_sub_log*# {
    set rvs [report_timing -from { #*dc_tcl_sub_from*# } -to { #*dc_tcl_sub_to*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    exit #*dc_tcl_exitcode*#
}
#+dc_tcl_measuremax_both_both_end+#
