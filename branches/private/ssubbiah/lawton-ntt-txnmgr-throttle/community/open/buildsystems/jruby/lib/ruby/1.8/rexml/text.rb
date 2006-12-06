require 'rexml/entity'
require 'rexml/doctype'
require 'rexml/child'
require 'rexml/doctype'
require 'rexml/parseexception'

module REXML
  # Represents text nodes in an XML document
  class Text < Child
    include Comparable
    # The order in which the substitutions occur
    SPECIALS = [ /&(?!#?[\w-]+;)/u, /</u, />/u, /"/u, /'/u, /\r/u ]
    SUBSTITUTES = ['&amp;', '&lt;', '&gt;', '&quot;', '&apos;', '&#13;']
    # Characters which are substituted in written strings
    SLAICEPS = [ '<', '>', '"', "'", '&' ]
    SETUTITSBUS = [ /&lt;/u, /&gt;/u, /&quot;/u, /&apos;/u, /&amp;/u ]

    # If +raw+ is true, then REXML leaves the value alone
    attr_accessor :raw

    ILLEGAL = /(<|&(?!(#{Entity::NAME})|(#0*((?:\d+)|(?:x[a-fA-F0-9]+)));))/um
    NUMERICENTITY = /&#0*((?:\d+)|(?:x[a-fA-F0-9]+));/ 

    # Constructor
    # +arg+ if a String, the content is set to the String.  If a Text,
    # the object is shallowly cloned.  
    #
    # +respect_whitespace+ (boolean, false) if true, whitespace is
    # respected
    #
    # +parent+ (nil) if this is a Parent object, the parent
    # will be set to this.  
    #
    # +raw+ (nil) This argument can be given three values.
    # If true, then the value of used to construct this object is expected to 
    # contain no unescaped XML markup, and REXML will not change the text. If 
    # this value is false, the string may contain any characters, and REXML will
    # escape any and all defined entities whose values are contained in the
    # text.  If this value is nil (the default), then the raw value of the 
    # parent will be used as the raw value for this node.  If there is no raw
    # value for the parent, and no value is supplied, the default is false.
    #   Text.new( "<&", false, nil, false ) #-> "&lt;&amp;"
    #   Text.new( "<&", false, nil, true )  #-> IllegalArgumentException
    #   Text.new( "&lt;&amp;", false, nil, true )  #-> "&lt;&amp;"
    #   # Assume that the entity "s" is defined to be "sean"
    #   # and that the entity    "r" is defined to be "russell"
    #   Text.new( "sean russell" )          #-> "&s; &r;"
    #   Text.new( "sean russell", false, nil, true ) #-> "sean russell"
    #
    # +entity_filter+ (nil) This can be an array of entities to match in the
    # supplied text.  This argument is only useful if +raw+ is set to false.
    #   Text.new( "sean russell", false, nil, false, ["s"] ) #-> "&s; russell"
    #   Text.new( "sean russell", false, nil, true, ["s"] ) #-> "sean russell"
    # In the last example, the +entity_filter+ argument is ignored.
    #
    # +pattern+ INTERNAL USE ONLY
    def initialize(arg, respect_whitespace=false, parent=nil, raw=nil, 
      entity_filter=nil, illegal=ILLEGAL )

      @raw = false

      if parent
        super( parent )
        @raw = parent.raw 
      else
        @parent = nil
      end

      @raw = raw unless raw.nil?
      @entity_filter = entity_filter
      @normalized = @unnormalized = nil

      if arg.kind_of? String
        @string = arg.clone
        @string.squeeze!(" \n\t") unless respect_whitespace
      elsif arg.kind_of? Text
        @string = arg.to_s
        @raw = arg.raw
      elsif
        raise "Illegal argument of type #{arg.type} for Text constructor (#{arg})"
      end

      @string.gsub!( /\r\n?/, "\n" )

      # check for illegal characters
      if @raw
        if @string =~ illegal
          raise "Illegal character '#{$1}' in raw string \"#{@string}\""
        end
      end
    end

    def node_type
      :text
    end

    def empty?
      @string.size==0
    end


    def clone
      return Text.new(self)
    end


    # Appends text to this text node.  The text is appended in the +raw+ mode
    # of this text node.
    def <<( to_append )
      @string << to_append.gsub( /\r\n?/, "\n" )
    end


    # +other+ a String or a Text
    # +returns+ the result of (to_s <=> arg.to_s)
    def <=>( other )
      to_s() <=> other.to_s
    end

    REFERENCE = /#{Entity::REFERENCE}/
    # Returns the string value of this text node.  This string is always
    # escaped, meaning that it is a valid XML text node string, and all
    # entities that can be escaped, have been inserted.  This method respects
    # the entity filter set in the constructor.
    #   
    #   # Assume that the entity "s" is defined to be "sean", and that the 
    #   # entity "r" is defined to be "russell"
    #   t = Text.new( "< & sean russell", false, nil, false, ['s'] ) 
    #   t.to_s   #-> "&lt; &amp; &s; russell"
    #   t = Text.new( "< & &s; russell", false, nil, false ) 
    #   t.to_s   #-> "&lt; &amp; &s; russell"
    #   u = Text.new( "sean russell", false, nil, true )
    #   u.to_s   #-> "sean russell"
    def to_s
      return @string if @raw
      return @normalized if @normalized

      doctype = nil
      if @parent
        doc = @parent.document
        doctype = doc.doctype if doc
      end

      @normalized = Text::normalize( @string, doctype, @entity_filter )
    end

    def inspect
      @string.inspect
    end

    # Returns the string value of this text.  This is the text without
    # entities, as it might be used programmatically, or printed to the
    # console.  This ignores the 'raw' attribute setting, and any
    # entity_filter.
    #
    #   # Assume that the entity "s" is defined to be "sean", and that the 
    #   # entity "r" is defined to be "russell"
    #   t = Text.new( "< & sean russell", false, nil, false, ['s'] ) 
    #   t.string   #-> "< & sean russell"
    #   t = Text.new( "< & &s; russell", false, nil, false )
    #   t.string   #-> "< & sean russell"
    #   u = Text.new( "sean russell", false, nil, true )
    #   u.string   #-> "sean russell"
    def value
      @unnormalized if @unnormalized
      doctype = nil
      if @parent
        doc = @parent.document
        doctype = doc.doctype if doc
      end
      @unnormalized = Text::unnormalize( @string, doctype )
    end
     
     def wrap(string, width, addnewline=false)
       # Recursivly wrap string at width.
       return string if string.length <= width
       place = string.rindex(' ', width) # Position in string with last ' ' before cutoff
       if addnewline then
         return "\n" + string[0,place] + "\n" + wrap(string[place+1..-1], width)
       else
         return string[0,place] + "\n" + wrap(string[place+1..-1], width)
       end
     end

    # Sets the contents of this text node.  This expects the text to be 
    # unnormalized.  It returns self.
    #
    #   e = Element.new( "a" )
    #   e.add_text( "foo" )   # <a>foo</a>
    #   e[0].value = "bar"    # <a>bar</a>
    #   e[0].value = "<a>"    # <a>&lt;a&gt;</a>
    def value=( val )
      @string = val.gsub( /\r\n?/, "\n" )
      @unnormalized = nil
      @normalized = nil
      @raw = false
    end
 
     def indent_text(string, level=1, style="\t", indentfirstline=true)
      return string if level < 0
       new_string = ''
       string.each { |line|
         indent_string = style * level
         new_line = (indent_string + line).sub(/[\s]+$/,'')
         new_string << new_line
       }
       new_string.strip! unless indentfirstline
       return new_string
     end
 
    def write( writer, indent=-1, transitive=false, ie_hack=false ) 
      s = to_s()
      if not (@parent and @parent.whitespace) then
        s = wrap(s, 60, false) if @parent and @parent.context[:wordwrap] == :all
        if @parent and not @parent.context[:indentstyle].nil? and indent > 0 and s.count("\n") > 0
          s = indent_text(s, indent, @parent.context[:indentstyle], false)
        end
        s.squeeze!(" \n\t") if @parent and !@parent.whitespace
      end
      writer << s
    end

    # FIXME
    # This probably won't work properly
    def xpath
      path = @parent.xpath
      path += "/text()"
      return path
    end

    # Writes out text, substituting special characters beforehand.
    # +out+ A String, IO, or any other object supporting <<( String )
    # +input+ the text to substitute and the write out
    #
    #   z=utf8.unpack("U*")
    #   ascOut=""
    #   z.each{|r|
    #     if r <  0x100
    #       ascOut.concat(r.chr)
    #     else
    #       ascOut.concat(sprintf("&#x%x;", r))
    #     end
    #   }
    #   puts ascOut
    def write_with_substitution out, input
      copy = input.clone
      # Doing it like this rather than in a loop improves the speed
      copy.gsub!( SPECIALS[0], SUBSTITUTES[0] )
      copy.gsub!( SPECIALS[1], SUBSTITUTES[1] )
      copy.gsub!( SPECIALS[2], SUBSTITUTES[2] )
      copy.gsub!( SPECIALS[3], SUBSTITUTES[3] )
      copy.gsub!( SPECIALS[4], SUBSTITUTES[4] )
      copy.gsub!( SPECIALS[5], SUBSTITUTES[5] )
      out << copy
    end

    # Reads text, substituting entities
    def Text::read_with_substitution( input, illegal=nil )
      copy = input.clone

      if copy =~ illegal
        raise ParseException.new( "malformed text: Illegal character #$& in \"#{copy}\"" )
      end if illegal
      
      copy.gsub!( /\r\n?/, "\n" )
      if copy.include? ?&
        copy.gsub!( SETUTITSBUS[0], SLAICEPS[0] )
        copy.gsub!( SETUTITSBUS[1], SLAICEPS[1] )
        copy.gsub!( SETUTITSBUS[2], SLAICEPS[2] )
        copy.gsub!( SETUTITSBUS[3], SLAICEPS[3] )
        copy.gsub!( SETUTITSBUS[4], SLAICEPS[4] )
        copy.gsub!( /&#0*((?:\d+)|(?:x[a-f0-9]+));/ ) {|m|
          m=$1
          #m='0' if m==''
          m = "0#{m}" if m[0] == ?x
          [Integer(m)].pack('U*')
        }
      end
      copy
    end

    EREFERENCE = /&(?!#{Entity::NAME};)/
    # Escapes all possible entities
    def Text::normalize( input, doctype=nil, entity_filter=nil )
      copy = input.clone
      # Doing it like this rather than in a loop improves the speed
      if doctype
        copy = copy.gsub( EREFERENCE, '&amp;' )
        doctype.entities.each_value do |entity|
          copy = copy.gsub( entity.value, 
            "&#{entity.name};" ) if entity.value and 
              not( entity_filter and entity_filter.include?(entity) )
        end
      else
        copy = copy.gsub( EREFERENCE, '&amp;' )
        DocType::DEFAULT_ENTITIES.each_value do |entity|
          copy = copy.gsub(entity.value, "&#{entity.name};" )
        end
      end
      copy
    end

    # Unescapes all possible entities
    def Text::unnormalize( string, doctype=nil, filter=nil, illegal=nil )
      rv = string.clone
      rv.gsub!( /\r\n?/, "\n" )
      matches = rv.scan( REFERENCE )
      return rv if matches.size == 0
      rv.gsub!( NUMERICENTITY ) {|m|
        m=$1
        m = "0#{m}" if m[0] == ?x
        [Integer(m)].pack('U*')
      }
      matches.collect!{|x|x[0]}.compact!
      if matches.size > 0
        if doctype
          matches.each do |entity_reference|
            unless filter and filter.include?(entity_reference)
              entity_value = doctype.entity( entity_reference )
              re = /&#{entity_reference};/
              rv.gsub!( re, entity_value ) if entity_value
            end
          end
        else
          matches.each do |entity_reference|
            unless filter and filter.include?(entity_reference)
              entity_value = DocType::DEFAULT_ENTITIES[ entity_reference ]
              re = /&#{entity_reference};/
              rv.gsub!( re, entity_value.value ) if entity_value
            end
          end
        end
        rv.gsub!( /&amp;/, '&' )
      end
      rv
    end
  end
end
