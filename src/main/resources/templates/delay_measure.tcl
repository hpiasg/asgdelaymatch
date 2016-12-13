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



#+measure_min_rise_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min rise_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_min_rise_rise_end+#

#+measure_min_rise_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min rise_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_min_rise_fall_end+#

#+measure_min_rise_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min rise_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_min_rise_both_end+#

#+measure_min_fall_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min fall_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_min_fall_rise_end+#

#+measure_min_fall_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min fall_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_min_fall_fall_end+#

#+measure_min_fall_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min fall_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_min_fall_both_end+#

#+measure_min_both_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min both_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_min_both_rise_end+#

#+measure_min_both_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min both_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_min_both_fall_end+#

#+measure_min_both_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -to { #*to_sub*# } -path full -delay min -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# min both_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_min_both_both_end+#

#+measure_max_rise_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max rise_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_max_rise_rise_end+#

#+measure_max_rise_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max rise_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_max_rise_fall_end+#

#+measure_max_rise_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -rise_from { #*from_sub*# } -to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max rise_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_max_rise_both_end+#

#+measure_max_fall_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max fall_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_max_fall_rise_end+#

#+measure_max_fall_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max fall_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_max_fall_fall_end+#

#+measure_max_fall_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -fall_from { #*from_sub*# } -to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max fall_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_max_fall_both_end+#

#+measure_max_both_rise_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -rise_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max both_from: { #*from_sub*# } rise_to: { #*to_sub*# }"
}
#+measure_max_both_rise_end+#

#+measure_max_both_fall_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -fall_to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max both_from: { #*from_sub*# } fall_to: { #*to_sub*# }"
}
#+measure_max_both_fall_end+#

#+measure_max_both_both_begin+#
redirect -append #*dc_sub_log*# {
    set rvs [report_timing -from { #*from_sub*# } -to { #*to_sub*# } -path full -delay max -nworst 1 -max_paths 1 -significant_digits 5 -sort_by group]
}
if {$rvs == 0} {
    echo "report_timing_fail #*root_sub*# max both_from: { #*from_sub*# } both_to: { #*to_sub*# }"
}
#+measure_max_both_both_end+#



#+final_begin+#
exit 2
#+final_end+#
