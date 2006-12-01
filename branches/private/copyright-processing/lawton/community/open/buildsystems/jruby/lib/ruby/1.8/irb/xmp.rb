#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

#
#   xmp.rb - irb version of gotoken xmp
#   	$Release Version: 0.9$
#   	$Revision$
#   	$Date$
#   	by Keiju ISHITSUKA(Nippon Rational Inc.)
#
# --
#
#   
#

require "irb"
require "irb/frame"

class XMP
  @RCS_ID='-$Id$-'

  def initialize(bind = nil)
    IRB.init_config(nil)
    #IRB.parse_opts
    #IRB.load_modules

    IRB.conf[:PROMPT_MODE] = :XMP

    bind = IRB::Frame.top(1) unless bind
    ws = IRB::WorkSpace.new(bind)
    @io = StringInputMethod.new
    @irb = IRB::Irb.new(ws, @io)
    @irb.context.ignore_sigint = false

#    IRB.conf[:IRB_RC].call(@irb.context) if IRB.conf[:IRB_RC]
    IRB.conf[:MAIN_CONTEXT] = @irb.context
  end

  def puts(exps)
    @io.puts exps

    if @irb.context.ignore_sigint
      begin
	trap_proc_b = trap("SIGINT"){@irb.signal_handle}
	catch(:IRB_EXIT) do
	  @irb.eval_input
	end
      ensure
	trap("SIGINT", trap_proc_b)
      end
    else
      catch(:IRB_EXIT) do
	@irb.eval_input
      end
    end
  end

  class StringInputMethod < IRB::InputMethod
    def initialize
      super
      @exps = []
    end

    def eof?
      @exps.empty?
    end

    def gets
      while l = @exps.shift
	next if /^\s+$/ =~ l
	l.concat "\n"
	print @prompt, l
	break
      end
      l
    end

    def puts(exps)
      @exps.concat exps.split(/\n/)
    end
  end
end

def xmp(exps, bind = nil)
  bind = IRB::Frame.top(1) unless bind
  xmp = XMP.new(bind)
  xmp.puts exps
  xmp
end
