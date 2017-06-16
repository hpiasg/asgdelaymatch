#+dc_sh_begin+#
#!/bin/sh
dc_shell -f "#*dc_tcl_file*#" > #*dc_log_file*#
exit $?
#+dc_sh_end+#