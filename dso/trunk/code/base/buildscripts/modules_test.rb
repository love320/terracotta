#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Adds methods to BuildSubtree that allow you to run JUnit tests on it. This is probably
# the most complex single file in the entire buildsystem.

# The prefix that we use for dynamically-generated properties -- that is, those that are
# created by the buildsystem out of its own internal information, and not simply information
# manually specified by the user and merely ferried across.
DYNAMICALLY_GENERATED_PROPERTIES_PREFIX = 'tc.tests.info.'

# The prefix that we use for statically-generated properties -- that is, those that are
# specified by the user (or monkey, or configuration file) and simply ferried across to
# the tests.
STATIC_PROPERTIES_PREFIX = 'tc.tests.configuration.'

class BuildSubtree
    # Creates a SubtreeTestRun object for this subtree and returns it. Most of the arguments
    # are self-explanatory (they're the obvious instances of the classes with the same names);
    # test_patterns is an array of Ant-style patterns (e.g., "**/*Test") that indicates
    # which tests should be run. aggregation_directory, if non-nil, is a directory into
    # which we should copy the XML result files for all tests. (This is used for
    # CruiseControl, since we want to be able to point it at a single directory of XML
    # log files from test runs, not one per subtree.)
    def test_run(static_resources, testrun_results, build_results, build_environment, config_source, jvm_set, ant, platform, test_patterns, aggregation_directory=nil)
        SubtreeTestRun.new(self, static_resources, buildconfig_properties,
            testrun_results, build_results, build_environment, config_source, jvm_set, ant, platform, test_patterns, aggregation_directory)
    end

    def container_home
      ENV['TC_CONTAINER_HOME']
    end

    def create_dynamic_property(key, value)
      "%s%s=%s" % [ DYNAMICALLY_GENERATED_PROPERTIES_PREFIX,
          key.to_s.to_propertyfile_escaped_s,
          value.to_s.to_propertyfile_escaped_s ]
    end

    # Creates a property file (in the style of java.util.Properties.load()) that contains all
    # the information necessary for the TestConfigObject to do its job and return the
    # appropriate information.
    def create_build_configuration_file(static_resources, testrun_results, build_results, build_environment, config_source, boot_jar, ant, jvm, jvmargs, timeout)
        File.open(testrun_results.build_configuration_file(self).to_s, "w") do |file|
            file << "# Dynamic properties: pieces of information generated by the buildsystem and passed to the Java code\n"
            # The root of the classname-based directory hierarchy used for temporary files created by tests
            write_dynamic_property(file, "temp-root", testrun_results.temp_dir(self))
            # The root of the classname-based directory hierarchy used for data files that tests need
            write_dynamic_property(file, "data-root", build_test_data_directory(testrun_results, ant))
            # The CLASSPATH that should be used for the LinkedChildProcess; this needs to be quite minimal (which
            # is why that class is pretty much in its own module), as it's in the CLASSPATH used to run application
            # servers -- and they typically don't like it if you put random stuff in their CLASSPATH that isn't
            # their own classes.
            write_dynamic_property(file, "linked-child-process-classpath", build_module.module_set['linked-child-process'].subtree('src').classpath(build_results, :full, :runtime))

            if container_home
                write_dynamic_property(file, "appserver.home", container_home)
            end

            # Builds up the set of classes required for DSO to support sessions
            sessionSet = PathSet.new
            sessionSet << build_module.module_set['dso-l1-session'].subtree('src').own_classes_only_classpath(build_results)
            sessionSet << build_module.module_set['dso-l1-tomcat'].subtree('src').own_classes_only_classpath(build_results)
            sessionSet << build_module.module_set['dso-l1-tomcat50'].subtree('src').own_classes_only_classpath(build_results)
            sessionSet << build_module.module_set['dso-l1-tomcat55'].subtree('src').own_classes_only_classpath(build_results)
            sessionSet << build_module.module_set['dso-l1-weblogic'].subtree('src').own_classes_only_classpath(build_results)
            sessionSet << build_module.module_set['dso-l1-session'].subtree('src').own_classes_only_classpath(build_results)

            # Writes out the location of the boot JAR for this tree, if one was created.
            write_dynamic_property(file, "bootjars.normal", boot_jar.path) unless boot_jar.nil?
            # Writes out the location of the session CLASSPATH that DSO needs
            write_dynamic_property(file, "session.classpath", sessionSet)
            # Writes out the location of the path that tests should use to search for executables; currently,
            # this is just precisely the native library path.
            write_dynamic_property(file, "executable-search-path", native_library_path(build_results, build_environment, :full))
            # The timeout the tests are going to use. This does *not* actually set the timeout;
            # that's set in the Ant <junit> task. Rather, this is so that the test can fire off
            # a thread dump before it's timed out.
            write_dynamic_property(file, "junit-test-timeout-inseconds", (timeout/1000))

            # The JVM version (e.g., "1.4.2_07"), type (e.g., "hotspot"), and mode (e.g., "client" or "server")
            # that should be used. This is used for a couple tests we have checked in, which make sure they're
            # running with the VM we think they should be running with. (Without them, we are just relying on
            # the buildsystem to spawn the right VM and get it right with no checks; this is a recipe for getting
            # it quite wrong. In fact, these tests already caught a bug; we used to run either with '-server' or
            # without '-server', but never with '-client'. 1.5 decides to pick a VM based on the machine's RAM
            # capacity, number of CPUs, and so on, and so it actually picked server even when we didn't request it.
            # Long story short, you should keep those tests around, and use this code, because it actually
            # really does make a difference.)
            write_dynamic_property(file, "jvm.version", jvm.actual_version)
            write_dynamic_property(file, "jvm.type", jvm.actual_type)
            write_dynamic_property(file, "jvm.mode", (jvmargs.any? { |x| x =~ /-server/i }) ? "server" : "client")

            # Write out which variant values are available for each variant name, and write out which libraries
            # should be included if the given variant is set to each of the possible values. Right now, this is
            # *all* that variants do -- they do not *ever* actually change the CLASSPATH of what we spawn, because
            # they're only needed for container tests right now (and container tests spawn their own VMs and thus
            # just read these properties directly).
            full_all_variants.each do |variant_name, variant_values|
              write_dynamic_property(file, "variants.available.%s" % variant_name, variant_values.join(","))
              variant_values.each do |variant_value|
                write_dynamic_property(file, "libraries.variants.%s.%s" % [ variant_name, variant_value ], full_variant_libraries(variant_name, variant_value))
              end
            end

            # Looks for selected variants, and writes out information about which variant is selected.
            config_source.keys.sort.each do |key|
              if key =~ /^variant\.([^\.]+)$/i
                variant_name = $1
                variant_value = config_source[key]

                raise RuntimeError, "There is no variant '%s' for '%s'!" % [ variant_value, variant_name ] unless full_all_variants[variant_name].include?(variant_value)
                write_dynamic_property(file, "variants.selected.%s" % variant_name, variant_value)
              end
            end

            # Writes out the 'short path temporary directory', which is used by the container code to put
            # app servers into a directory that has a shorter pathname prefix. This is so that Windows,
            # which sucks all kinds of ass, doesn't screw us with its limitations on pathname length.
            if build_environment.has_pathname_length_limitations? && (! config_source['SHORT_PATH_TEMPDIR'].blank?)
                write_dynamic_property(file, "short-path-tempdir", config_source['SHORT_PATH_TEMPDIR'])
            end

            # Writes out all static properties -- these are ones that start with 'tc.tests.configuration.', and
            # are merely passed through from the configuration source (typically the command line and build-config.(local|global)
            # files) to the TestConfigObject).
            file << "\n"
            file << "# Static properties: those specified manually by the user or CruiseControl configuration, not the buildsystem\n"
            write_static_properties(file, config_source)
        end
    end

    protected
    # Copies all test data for this subtree to the given directory.
    def copy_subtree_test_data_to_directory(dest_directory, ant)
        out = 0

        if FileTest.exist?(test_data_source_directory.to_s)
            out += 1
            ant.copy(:todir => dest_directory.to_s) {
                ant.fileset(:dir => test_data_source_directory.to_s)
            }
        end

        out
    end

    # Copies all test data for this subtree, and for any subtrees in the same build module that
    # it's dependent upon, to the given directory.
    def copy_module_test_data_to_directory(dest_directory, ant)
        out = 0

        @internal_dependencies.each do |internal_dependency|
            out += build_module.subtree(internal_dependency).copy_subtree_test_data_to_directory(dest_directory, ant)
        end

        out += copy_subtree_test_data_to_directory(dest_directory, ant)
        out
    end

    private
    # Writes out a 'dynamic' property -- one starting with the DYNAMICALLY_GENERATED_PROPERTIES_PREFIX --
    # to the given file.
    def write_dynamic_property(file, key, value)
        file << "#{create_dynamic_property(key, value)}\n"
    end

    # Writes out all properties that start with the STATIC_PROPERTIES_PREFIX from the given
    # configuration source to the given file.
    def write_static_properties(file, config_source)
        config_source.keys.each do |key|
            if key.starts_with?(STATIC_PROPERTIES_PREFIX)
                file << "%s=%s\n" % [ key.to_s.to_propertyfile_escaped_s, config_source[key].to_s.to_propertyfile_escaped_s ]
            end
        end
    end

    # Where is the build configuration file, that contains settings that affect how we
    # run tests on this subtree?
    def test_buildconfig_file
        FilePath.new(build_module.root, @name + ".buildconfig")
    end

    # The directory that data files for this subtree are found in.
    def test_data_source_directory
        FilePath.new(build_module.root, @name + ".data")
    end

    # Computes the build-configuration properties, which affect how we run tests on this
    # subtree. These are read from a properties-style file at <module>/<subtree>.buildconfig,
    # if it exists; otherwise, they're empty. Returns a hash.
    def buildconfig_properties
        if @buildconfig_properties.nil?
            @buildconfig_properties = { }
            filepath = test_buildconfig_file.to_s
            if File.file?(filepath)
                File.open(filepath) do |file|
                    lineno = 1
                    file.each do |line|
                        if line =~ /^\s*(\S[^=]+?)\s*=\s*(\S.*?)\s*$/
                            @buildconfig_properties[$1] = $2
                        elsif line =~ /^\s*#.*$/ || line =~ /^\s*$/
                            # Nothing here
                        else
                            raise RuntimeError, "%s:%d: Line is an invalid format: %s" % [ filepath, lineno, line ]
                        end

                        lineno += 1
                    end
                end
            end
        end

        @buildconfig_properties
    end

    # Builds the test-data directory, which contains test data for this subtree and any
    # subtree this subtree is dependent upon. This is so that test code can ask for the data
    # directory and know it'll get its data, rather than having to make sure it's only when
    # it's running tests from the actual subtree it's in.
    def build_test_data_directory(testrun_results, ant)
        dir = testrun_results.tests_data_dir(self)

        directories_copied_from = copy_all_test_data_to_directory(dir, ant)
        if directories_copied_from > 0
            puts "Compiled test data from %d director%s into '%s' for tests on '%s/%s'." %
            [ directories_copied_from, directories_copied_from == 1 ? "y" : "ies", dir.to_s, build_module.name, name ]
        else
            puts "No data directories found for '%s/%s', or its dependencies." % [ build_module.name, name ]
        end
        dir
    end

    # Copies all test data to the given directory, from this subtree and all the subtrees
    # it's dependent upon.
    def copy_all_test_data_to_directory(dest_directory, ant)
        out = 0

        build_module.dependent_modules.each do |dependent_module|
            out += dependent_module.subtree(@external_dependencies_like).copy_module_test_data_to_directory(dest_directory, ant)
        end

        out += copy_module_test_data_to_directory(dest_directory, ant)
        out
    end
end

# A SubtreeTestRun object is what actually runs tests on a tree. Basically, you ask a BuildSubtree
# to create one for you, and you can then use it to set up for tests, run tests, prepare to run
# them externally (i.e., in Eclipse), or have it print out how it would run tests. This therefore
# encapsulates all test set-up, run, and tear-down logic into a single place so that you can do
# whatever you want with it.
class SubtreeTestRun
    include_class('java.lang.System') { |p, name| "Java" + name }

    # The default timeout for tests, in seconds. Currently, this is 15 minutes.
    DEFAULT_TEST_TIMEOUT_SECONDS = 15 * 60

    # Creates a new instance. Most of the parameters should be obvious; they're just references
    # to the obvious instances of the similarly-named classes. buildconfig is the build-configuration
    # hash (see BuildSubtree#buildconfig_properties, above); test_patterns is an array of Ant-style
    # patterns indicating which tests should be run, and aggregation_directory is a directory to
    # copy the result XML files into when we're done running tests.
    def initialize(subtree, static_resources, buildconfig, testrun_results, build_results, build_environment, config_source, jvm_set, ant, platform, test_patterns, aggregation_directory)
        @subtree = subtree
        @build_module = @subtree.build_module
        @static_resources = static_resources
        @buildconfig = buildconfig
        @testrun_results = testrun_results
        @test_patterns = test_patterns
        @build_results = build_results
        @build_environment = build_environment
        @config_source = config_source
        @ant = ant
        @platform = platform
        @aggregation_directory = aggregation_directory

        @use_dso_boot_jar = buildconfig['include-dso-boot-jar'] =~ /^\s*true\s*$/i
        @needs_dso_boot_jar = @use_dso_boot_jar || (buildconfig['build-dso-boot-jar'] =~ /^\s*true\s*$/i)
        @timeout = (config_source["test_timeout"] || buildconfig["timeout"] || DEFAULT_TEST_TIMEOUT_SECONDS.to_s).to_i * 1000

        @extra_jvmargs = config_source.as_array('jvmargs') || []
        if buildconfig['jvmargs']
          jvmargs = buildconfig['jvmargs'].split(/\s*,\s*/)
          # Make sure the heap size settings in the buildconfig override the
          # global heap size settings.
          jvmargs.each do |jvmarg|
            if match = /-Xm([sx])/.match(jvmarg)
              @extra_jvmargs.delete_if { |arg| arg =~ /-Xm#{match[1]}/ }
            end
            @extra_jvmargs << jvmarg
          end
        end

        if test_props = buildconfig['test.tc.properties']
          props_file = FilePath.new(@build_results.classes_directory(@subtree), test_props).canonicalize
          @extra_jvmargs << "-Dcom.tc.properties=#{props_file.to_propertyfile_escaped_s}"
        end
    end

    # Returns true if this test run requires a container to run.
    def requires_container?
      @requires_container ||= @buildconfig['requires-container'] =~ /^\s*true\s*$/i
    end

    # Does all preparations necessary to run the given set of tests.
    def setUp

        @has_tests = @subtree.source_exists
        if @has_tests
            @found_tests = find_tests
            @has_tests = ! @found_tests.empty?
        end

        # Skip preparations if we're not actually running any tests on this tree; doing things
        # like building the DSO boot JAR takes time, and it's pointless if we're not actually
        # going to run any tests on this tree (which we might not do depending on how the
        # patterns are set...).
        if ! @has_tests
            @setUp = true
            return
        end

        # tests_jvm will raise an exception if there is a problem with the JVM configuration
        tests_jvm

        puts "------------------------------------------------------------------------"
        puts "PREPARING to run tests (%s) on subtree '%s/%s'..." % [ @test_patterns.join(", "), @subtree.build_module.name, @subtree.name ]
        puts ""

        # Build a DSO boot JAR, if necessary.
        boot_jar = nil
        if @needs_dso_boot_jar
            puts "This subtree requires a DSO boot JAR to run tests. Building one."
            module_set = @subtree.build_module.module_set

            boot_jar = BootJar.new(@build_results, tests_jvm,
                @testrun_results.boot_jar_directory(@subtree),
                module_set, @ant, @platform,
                @subtree.boot_jar_config_file(@static_resources).to_s)
            boot_jar.ensure_created
        end

        # This is necessary to make Log4J behave correctly. Ah, Log4J is insane.
        if FileTest.exist?(@static_resources.log4j_properties_file.to_s)
            @ant.copy(:file => @static_resources.log4j_properties_file.to_s, :todir => @testrun_results.temp_dir(@subtree).to_s)
        end

        # This creates the file that TestConfigObject reads.
        @subtree.create_build_configuration_file(@static_resources, @testrun_results, @build_results, @build_environment, @config_source, boot_jar, @ant, tests_jvm, all_jvmargs, @timeout)

        native_library_path = @subtree.native_library_path(@build_results, @build_environment, :full)

        @jvmargs = [ ]

        # 'tc.tests.info.property-files' is set so that TestConfigObject knows which file to go read.
        @sysproperties = {
            "tc.base-dir" => @static_resources.root_dir.to_s,
            'java.awt.headless' => true,
            'tc.tests.info.property-files' => @testrun_results.build_configuration_file(@subtree).to_s
        }

        @sysproperties['java.library.path'] = native_library_path.to_s unless native_library_path.to_s.blank?

        if @use_dso_boot_jar
            @jvmargs << '-Xbootclasspath/p:%s' % boot_jar.path.to_s
            @sysproperties.merge!({
                'tc.config' => @static_resources.dso_test_runtime_config_file,
                'tc.dso.globalmode' => false
                })
        end

        # We run the tests with CWD set to the temporary directory, just in case the
        # test decides to just new up files directly (without using the temporary-directory
        # stuff) and write them.
        @cwd = @testrun_results.temp_dir(@subtree)
        @classpath = @subtree.classpath(@build_results, :full, :runtime)

        # This is *quite* important. If something goes really wrong with a test, to the point where it
        # crashes, doesn't even get started, hangs hard-core, or otherwise can't write out its result
        # XML file, then, without this, we'll never know about it -- which is really, really bad.
        # Instead, we write out these "test didn't run" XML files ahead of time, and let the tests
        # overwrite them as they go; this way, it's positive, instead of negative, feedback -- we only
        # count the test as having passed if we *know* it passed, rather than only counting it as
        # having failed if we *know* it failed. Much better.
        puts "Writing out 'did-not-run' XML files for the %d test(s) in %s/%s..." % [ @found_tests.size, @subtree.build_module.name, @subtree.name ]
        @found_tests.each do |found_test|
            class_name = @build_results.class_name_for_class_file(@subtree, found_test)
            create_did_not_run_file(class_name, @testrun_results.results_file(@subtree, class_name)) unless FilePath.new(found_test).filename =~ /\$/
        end

        # Grep for current java processes for debugging
        path = File.join(@cwd.to_s, "javaprocesses.txt")
        File.open(path, "w") do |file|
            file << ps_grep_java
        end

        puts "Done."

        @setUp = true
    end

    # The list of system properties that *must* be set directly on the spawned JVM, rather than
    # being able to be set by TestConfigObject calling System.setProperty() from its static
    # initializer block. These are system properties that the JVM itself reads, or that DSO
    # (which loads from the bootclasspath, long, long before TestConfigObject loads) uses.
    NON_CLASSPATH_LOADABLE_SYSTEM_PROPERTIES = [ 'java.library.path', 'tc.config', 'tc.dso.globalmode' ]

    # Prepares to have tests in this tree run externally (i.e., but Eclipse). This mostly just
    # sets up a properties file that contains a list of system properties for TestConfigObject
    # to set when it's initialized, plus writes out a 'stamp' file indicating which tree
    # we've prepared the tests for.
    #
    # (TestConfigObject loads its file using TestConfigObject.class.getResource(); as such,
    # it loads this file from its own class tree in the filesystem, and so we overwrite it
    # no matter what subtree we're preparing a test run for. Because different subtrees are
    # configured differently and thus will put different things into this file, we have to
    # keep track of what tree we're currently prepared for so that the Eclipse tool that
    # runs Terracotta tests from Eclipse knows whether or not to re-run 'check_prep'.
    #
    # Yes, people have bitched and moaned about this, but it's not remotely clear to me that
    # there's an easier way. If you mandated -- and could enforce -- that every single last
    # test class in the system inherited from TCTestCase, you could have TestConfigObject
    # fetch its resource based on that class, and therefore load the right file...I think.
    # There's still some discussion about whether or not getResource() only looks in the part
    # of the CLASSPATH where the class you called it on was loaded, or if it re-searches
    # the whole CLASSPATH from the top every time. If the latter, then you're *really* screwed.)
    def prepare_for_external_run
        raise "You must call setUp before running this method." unless @setUp

        if ! @has_tests
            return
        end

        test_config_system_properties_file = @build_results.test_config_system_properties_file(@subtree.build_module.module_set)

        # Write out the system properties that we need to set.
        File.open(test_config_system_properties_file.to_s, "w") do |file|
            @sysproperties.each do |key, value|
                file << "%s=%s\n" % [ key.to_s.to_propertyfile_escaped_s, value.to_s.to_propertyfile_escaped_s ]
            end
        end

        # Compute which system properties need to be set manually.
        required_system_properties = @sysproperties.keys & NON_CLASSPATH_LOADABLE_SYSTEM_PROPERTIES

        # Write out a file 'stamping' the module and subtree we've prepared this for, plus containing
        # extra information that the Eclipse tool we wrote that runs Terracotta tests needs in order
        # to correctly spawn the JVM that will run the test(s).
        File.open(@build_results.prepped_stamp_file(@subtree.build_module.module_set).to_s, "w") do |file|
            file << "# This file is an indication that 'tcbuild check_prep' has been run.\n"
            file << "tcbuild.prepared.module=%s\n" % @subtree.build_module.name.to_propertyfile_escaped_s
            file << "tcbuild.prepared.subtree=%s\n" % @subtree.name.to_propertyfile_escaped_s
            file << "tcbuild.prepared.cwd=%s\n" % @cwd.to_s.to_propertyfile_escaped_s
            file << "tcbuild.prepared.jvm.java=%s\n" % tests_jvm.java.to_s.to_propertyfile_escaped_s
            file << "tcbuild.prepared.jvm.version=%s\n" % tests_jvm.actual_version.to_propertyfile_escaped_s
            file << "tcbuild.prepared.jvm.type=%s\n" % tests_jvm.actual_type.to_propertyfile_escaped_s

            jvm_args = all_jvmargs
            if container_home = @subtree.container_home || @config_source['tc.tests.configuration.appserver.home']
              jvm_args << "-D#{@subtree.create_dynamic_property('appserver.home', container_home)}"
            end
            file << "tcbuild.prepared.jvmargs=%s\n" % jvm_args.length.to_s.to_propertyfile_escaped_s

            index = 0
            jvm_args.each do |jvmarg|
                file << "tcbuild.prepared.jvmarg_%d=%s\n" % [ index, jvmarg.to_propertyfile_escaped_s ]
                index += 1
            end


            required_system_properties.each do |syspropertykey|
                file << "tcbuild.prepared.system-property.%s=%s\n" % [ syspropertykey.to_propertyfile_escaped_s, @sysproperties[syspropertykey].to_s.to_propertyfile_escaped_s ]
            end
        end

        puts "========================================================================"
        puts "Wrote required system properties for module %s/%s to:" % [ @subtree.build_module.name, @subtree.name]
        puts "  '%s'." % test_config_system_properties_file.to_s
        puts "The test configuration system will automatically load this file as needed."

        extra = ""
        extra += "  JVM arguments:               %s\n" % all_jvmargs.join(" ") unless all_jvmargs.empty?

        unless required_system_properties.empty?
            extra += "  System properties:           \n"
            required_system_properties.each { |key| extra += "          -D%s=%s\n" % [ key, @sysproperties[key] ] }
        end

        unless extra.blank?
            puts ""
            puts :warn, "This tree requires certain arguments and system properties that "
            puts :warn, "can't be set at runtime. You should set these directly via your "
            puts :warn, "IDE. If you don't, your tests may or may not work -- you have "
            puts :warn, "been warned."
            puts ""
            puts extra
        end

        puts ""
        puts "And, just FYI (it isn't usually necessary to set these) the buildsystem "
        puts "normally runs tests in %s/%s..." % [ @subtree.build_module.name, @subtree.name ]
        puts "   ...with the current working directory set to '%s'." % @cwd.to_s
        puts "   ...using the Java command at '%s'." % tests_jvm.java.to_s
        puts ""
    end

    @@next_failure_property_sequence = 1

    # Runs the tests. script_results is the ScriptResults object, which we inform of any
    # failures we run into.
    def run(script_results)
        raise "You must call setUp before running this method." unless @setUp

        return unless @has_tests

        puts ""
        puts "========================================================================"
        puts "RUNNING tests (%s) on %s/%s..." % [ @test_patterns.join(", "), @subtree.build_module.name, @subtree.name ]
        puts ""

        failed = false
        failure_properties = [ ]

        # Run the tests. Most of the real magic here comes in the 'splice_into_ant_junit'
        # method, which puts the necessary <jvmarg>, <sysproperty>, and so forth elements
        # into the junit task.
        @ant.junit(
        :timeout => @timeout,
        :dir => @cwd.to_s,
        :tempdir => @testrun_results.ant_temp_dir(@subtree).to_s,
        :fork => true,
        :showoutput => true,
        :jvm => tests_jvm.java.to_s) {
            splice_into_ant_junit

            # Use our two formatters -- the first for the XML output files, the second for
            # printing output.
            @ant.formatter(:type => 'xml')
            @ant.formatter(:classname => 'com.tc.test.TCJUnitFormatter', :usefile => false)

            # Create a <batchtest> element for each pattern we have.
            @test_patterns.each do |pattern|
                failure_property_name = "tests_failed_%d" % @@next_failure_property_sequence
                @@next_failure_property_sequence += 1
                failure_properties << failure_property_name

                @ant.batchtest(:todir => @testrun_results.results_dir(@subtree).to_s, :fork => true, :failureproperty => failure_property_name) {
                    @ant.formatter(:classname => "com.tc.test.TCXMLJUnitFormatter", :usefile => false)
                    @ant.fileset(:dir => @build_results.classes_directory(@subtree).to_s, :includes => "**/#{pattern}.class")
                }
            end
        }

        # Check the failures by looking for the properies we set under failure_property_name, above.
        failure_properties.each { |property_name| failed = failed || (@ant.get_ant_property(property_name) != nil) }
        script_results.failed("Execution of tests in subtree '%s/%s' failed." % [ @subtree.build_module.name, @subtree.name ]) if failed

        # Aggregate the results into the aggregation directory, if it's set.
        unless @aggregation_directory.nil?
            puts "Copying test result files to '%s'..." % @aggregation_directory.to_s
            @ant.copy(:todir => @aggregation_directory.to_s) {
                @ant.fileset(:dir => @testrun_results.results_dir(@subtree).to_s, :includes => '*.xml')
            }

            Dir.open(@aggregation_directory.to_s).each do | file |

                next if File.size(FilePath.new(@aggregation_directory.to_s, file).to_s) > 0
                next unless file =~ /\.xml$/
                classname = file.scan(/TEST-(.+)\.xml/).join("")

                content = <<END
<?xml version="1.0" encoding="UTF-8" ?>
<testsuite errors="1" failures="0" name="#{classname}" tests="1" time="69">
  <properties>
    <property name="unknow" value="none"></property>
  </properties>
  <testcase classname="#{classname}" name="test" time="69"></testcase>
  <system-out><![CDATA[Test #{classname} failed. JVM crashed. Error parsing XML: Premature end of file.]]></system-out>
  <system-err><![CDATA[Test #{classname} failed. JVM crashed. Error parsing XML: Premature end of file.]]></system-err>
</testsuite>
END

                File.open(FilePath.new(@aggregation_directory.to_s, file).to_s, "w")  do | f |
                    f << content
                end
            end
        end
    end

    # Call this when you're all done with tests. It doesn't do anything yet, but it well may in the future.
    def tearDown
        # nothing yet
    end

    # Prints out the arguments and system properties we'd use when running tests on this tree.
    # This can be useful if you want to do something like run the test manually from the
    # command line (e.g., by invoking 'java' directly).
    def dump
        out = "\n\n========================================================================\n"
        out += "When running tests on subtree %s/%s, the buildsystem will use the following:\n\n" % [ @subtree.build_module.name, @subtree.name ]
        out += "  JVM arguments:               %s\n" % (all_jvmargs.empty? ? "<none>" : all_jvmargs.join(" "))

        if @sysproperties.empty?
            out += "  System properties:           <none>\n"
        else
            out += "  System properties:           \n"
            @sysproperties.each { |key, value| out += "          -D%s=%s\n" % [ key, value ] }
        end

        out += "  Timeout:                     %s milliseconds\n" % @timeout
        out += "  Current working directory:   %s\n" % @cwd.to_s
        out += "  'java' command:              %s\n\n" % tests_jvm.java.to_s
        out += "  CLASSPATH:\n\n%s\n\n" % @classpath

        out
    end

    # Which JVM should we use for this set of tests?
    def tests_jvm(jvm_set = Registry[:jvm_set])
      return @jvm if @jvm

      candidate_jvm = jvm_set['tests-jdk'] || jvm_set['jdk'] ||
                      jvm_set[@buildconfig['tests-jdk']] || jvm_set[@buildconfig['jdk']]
      if candidate_jvm
        override = true
      else
        candidate_jvm = @build_module.jdk
        override = false
      end

      if requires_container?
        current_appserver = Registry[:appserver_generic]
        compatibility = Registry[:appserver_compatibility][current_appserver] || {
          'min_version' => JavaVersion::JAVA_MIN_VERSION,
          'max_version' => JavaVersion::JAVA_MAX_VERSION
        }
        min_version = JavaVersion.new(compatibility['min_version'])
        max_version = JavaVersion.new(compatibility['max_version'])
        if candidate_jvm.version < min_version || candidate_jvm.version > max_version
          if override
            raise(JvmVersionMismatchException,
                  "JDK specified is incompatible with #{Registry[:appserver]},\n " +
                  "which requires minimum version #{min_version} and maximum " +
                  "version #{max_version}")
          else
            if appserver_candidate_jvm = jvm_set.find_jvm(
                  :min_version => compatibility['min_version'],
                  :max_version => compatibility['max_version'])
              candidate_jvm = appserver_candidate_jvm
            else
              raise(JvmVersionMismatchException,
                    "Could not find JDK compatible with #{Registry[:appserver]},\n" +
                    "which requires minimum version #{min_version} and maximum " +
                    "version #{max_version}")
            end
          end
        end
      end

      if candidate_jvm.version < @build_module.jdk.min_version
        raise(JvmVersionMismatchException,
              "JDK specified\n\t#{candidate_jvm}\nis incompatible with module " +
              "\n\t#{@build_module}\n which requires minimum version #{@build_module.jdk.min_version}")
      end
      @jvm = candidate_jvm
    end

  private
    # Splice the appropriate elements (CLASSPATH, JVM arguments, system properties,
    # and so on) into Ant.
    def splice_into_ant_junit
        @ant.classpath {
            @ant.pathelement(:path => @classpath.to_s)
        }

        @jvmargs.each do |jvmarg|
            @ant.jvmarg(:value => jvmarg)
        end

        @sysproperties.each do |key, value|
            @ant.sysproperty(:key => key, :value => value)
        end

        @extra_jvmargs.each do |jvmarg|
            @ant.jvmarg(:value => jvmarg)
        end
    end

    # A description of the patterns we're going to run tests with.
    def patterns_description
        out = ""
        @test_patterns.each do |pattern|
            out += ", " unless out.blank?
            out += "'" + pattern + "'"
        end
        out
    end

    # Figure out which tests we're going to run, as a PathSet.
    def find_tests
        @subtree.classes_matching_patterns(@test_patterns, @ant, @platform, @build_results)
    end

    # The message we write into the 'did-not-run' XML file.
    NOT_RUN_MESSAGE = 'This test, \'%s\', DID NOT RUN. Some earlier test or problem ' +
    'in the build must\'ve caused this problem. (This message comes ' +
    'from an XML file written by the buildsystem *before* each test ' +
    'is run.)'

    # The set of cached Java system properties.
    $cachedJavaProperties = nil

    # Returns a (possibly cached) set of all Java system properties, as a hash. (Caching is
    # safe, because we don't ever set Java system properties from our code.)
    def all_java_properties
        if $cachedJavaProperties.nil?
            $cachedJavaProperties = { }
            iterator = JavaSystem.getProperties.entrySet.iterator
            while iterator.hasNext
                entry = iterator.next
                $cachedJavaProperties[entry.getKey] = entry.getValue
            end
        end

        $cachedJavaProperties
    end

    # Creates a 'did-not-run' XML file for the test with the given class name, and put it in the
    # given target file. This is overwritten by the test once it completes (whether it fails or
    # succeeds); we write it out so that we can make sure we catch the case where the test never
    # completes.
    def create_did_not_run_file(class_name, target_file)
        File.open(target_file.to_s, "w") do |file|
            file << "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            file << "<testsuite errors=\"0\" failures=\"1\" name=\"%s\" tests=\"0\" time=\"0.000\">\n" % class_name.xml_escape(true)
            file << "  <properties>\n"

            properties = all_java_properties
            properties.merge!(@ant.all_ant_properties)

            properties.each do |key, value|
                value ||= ""
                file << "    <property name=\"%s\" value=\"%s\"></property>\n" % [ key.xml_escape(true), value.xml_escape(true) ]
            end

            file << "  </properties>\n"
            file << ("  <failure message=\"" + NOT_RUN_MESSAGE + "\">\n") % class_name.xml_escape(true)
            file << ("      " + NOT_RUN_MESSAGE + "\n") % class_name.xml_escape
            file << "   </failure>\n"
            file << "</testsuite>\n"
        end
    end

    # What JVM arguments should we use for these tests?
    def all_jvmargs
        out = @jvmargs || [ ]
        out += @extra_jvmargs unless @extra_jvmargs.empty?
        out
    end

    # do a "ps auxwwww | grep java"
    # to be used in monkey environment ONLY
    def ps_grep_java
        ps_cmd = case @build_environment.os_type(:nice)
            when /windows/i: 'pv.exe -l | grep java | grep -v grep'
            when /solaris/i: '/usr/ucb/ps auxwwww | grep java | grep -v grep'
            else 'ps auxwwww | grep java | grep -v grep'
        end

        begin
            java_processes = `#{ps_cmd}`
        rescue
            java_processes = ''
        end
        java_processes
    end
end
