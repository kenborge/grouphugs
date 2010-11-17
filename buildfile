# Generated by Buildr 1.3.4, change to your liking
############################################################################
# Project properties
############################################################################
VERSION_NUMBER = "1.0.0"
GROUP = "gh"
COPYRIGHT = "copyleft"

Buildr.settings.build['scala.version'] = "2.8.0"
require 'buildr/scala'

# Specify Maven 2.0 remote repositories here, like this:
repositories.remote << "http://www.ibiblio.org/maven2/"

desc "A pircbot implementation whose sole purpose is creating fun, happiness and grouped hugs"
define "gh" do
  ############################################################################
  # Project build properties
  ############################################################################
  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT

  ############################################################################
  # Compilation
  ############################################################################
  # target java 5 (not 6, because scala cannot target java 6)
  compile.options.target = '1.5'

  # compile against maven artifacts
  compile.with artifacts(:sqlite, :pircbot, :jdom, :tagsoup, :jaxen, :jchardet, :commonsio, :joda, :scalalib)


  ############################################################################
  # Packaging
  ############################################################################
  # package against maven artifacts
  package(:jar).with :manifest => {"Main-Class" => "no.kvikshaug.gh.Grouphug",
                                   "Class-Path" => artifacts(:sqlite, :pircbot, :jdom, :tagsoup, :jaxen, :jchardet, :commonsio, :joda, :scalalib).each(&:invoke).map(&:name).join(" ")  }
end
