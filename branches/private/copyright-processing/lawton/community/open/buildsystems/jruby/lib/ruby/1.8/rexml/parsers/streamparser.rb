#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module REXML
	module Parsers
		class StreamParser
			def initialize source, listener
				@listener = listener
				@parser = BaseParser.new( source )
			end

      def add_listener( listener )
        @parser.add_listener( listener )
      end

			def parse
				# entity string
				while true
					event = @parser.pull
					case event[0]
					when :end_document
						return
					when :start_element
						attrs = event[2].each do |n, v|
							event[2][n] = @parser.unnormalize( v )
						end
						@listener.tag_start( event[1], attrs )
					when :end_element
						@listener.tag_end( event[1] )
					when :text
						normalized = @parser.unnormalize( event[1] )
						@listener.text( normalized )
					when :processing_instruction
						@listener.instruction( *event[1,2] )
          when :start_doctype
            @listener.doctype( *event[1..-1] )
					when :comment, :attlistdecl, :notationdecl, :elementdecl, 
            :entitydecl, :cdata, :xmldecl, :attlistdecl
						@listener.send( event[0].to_s, *event[1..-1] )
					end
				end
			end
		end
	end
end
