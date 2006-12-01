#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

module SM

  ## 
  # Handle common directives that can occur in a block of text:
  #
  # : include : filename
  #

  class PreProcess

    def initialize(input_file_name, include_path)
      @input_file_name = input_file_name
      @include_path = include_path
    end

    # Look for common options in a chunk of text. Options that
    # we don't handle are passed back to our caller
    # as |directive, param| 

    def handle(text)
      text.gsub!(/^([ \t#]*):(\w+):\s*(.+)?\n/) do 
        prefix    = $1
        directive = $2.downcase
        param     = $3

        case directive
        when "include"
          filename = param.split[0]
          include_file(filename, prefix)

        else
          yield(directive, param)
        end
      end
    end

    #######
    private
    #######

    # Include a file, indenting it correctly

    def include_file(name, indent)
      if (full_name = find_include_file(name))
        content = File.open(full_name) {|f| f.read}
        res = content.gsub(/^#?/, indent)
      else
        $stderr.puts "Couldn't find file to include: '#{name}'"
        ''
      end
    end

    # Look for the given file in the directory containing the current
    # file, and then in each of the directories specified in the
    # RDOC_INCLUDE path

    def find_include_file(name)
      to_search = [ File.dirname(@input_file_name) ].concat @include_path
      to_search.each do |dir|
        full_name = File.join(dir, name)
        stat = File.stat(full_name) rescue next
        return full_name if stat.readable?
      end
      nil
    end

  end
end
