
# this script is called by build-terracotta.xml to svn update
# it will try to call "svn update" 3 times before crapping out
require 'tmpdir'
require 'yaml'

class SvnUpdate

  def initialize(monkey_name)
    @monkey_name = monkey_name
    # get path to top folder of the repo
    @topdir = File.join(File.expand_path(File.dirname(__FILE__)), "..", "..")
    @topdir = File.join(@topdir, "..") if @topdir =~ /community/

    # default build-archive-dir in monkeys
    build_archive_dir = "/shares/monkeyoutput"

    if ENV['OS'] =~ /win/i
      @topdir=`cygpath -u #{@topdir}`
      build_archive_dir = "o:"    
    end

    @good_rev_file = File.join(build_archive_dir, "currently_good_rev.txt")
    
    clean_up_temp_dir
  end

  def get_current_rev
    YAML::load(`svn info #{@topdir}`)["Last Changed Rev"].to_i
  end

  def svn_update_with_error_tolerant(revision)
    error_msg=''  
    3.downto(1) do 
      error_msg=`svn update #{@topdir} -r #{revision} -q --non-interactive 2>&1`
      return if $? == 0
      sleep(5*60)
      `svn cleanup #{@topdir}`
    end  
    fail(error_msg)
  end

  def get_current_good_rev(file)
    currently_good_rev = 0
    begin
      File.open(file, "r") do | f |
        currently_good_rev = f.gets.to_i    
      end
    rescue
      currently_good_rev = 0
    end
    currently_good_rev
  end

  def clean_up_temp_dir
    # clean out temp dir
    `rm -rf #{Dir.tmpdir}/terracotta*`
    `rm -rf /var/tmp/terracotta*`
    `rm -rf #{Dir.tmpdir}/open*`
    `rm -rf #{Dir.tmpdir}/*.dat`
    `rm -rf #{Dir.tmpdir}/sprint*`
  end
  
  def update
    while true
      current_rev = get_current_rev()
      current_good_rev = get_current_good_rev(@good_rev_file)
      
      puts "curr: #{current_rev}"
      puts "good: #{current_good_rev}"

      if @monkey_name == "general-monkey" || @monkey_name == "test-monkey"
        svn_update_with_error_tolerant("HEAD")
        exit(0)
      elsif current_rev <= current_good_rev
        svn_update_with_error_tolerant(current_good_rev)
        exit(0)
      else # I have a revision that is greater than a good known reivision, so I sleep and wait
        sleep(5*60)
      end
    end
  end
  
end # class SvnUpdate

svn = SvnUpdate.new(ARGV[0])
svn.update
