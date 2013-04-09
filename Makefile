############################################################
#  Small makefile for building PRISM source distributions  #
############################################################

default: none

none:
	@echo 'Did you want to build PRISM? Do "cd prism" and then "make"'

# By default, extract version number from Java code using printversion
# Can be overridden by passing VERSION=xxx
VERSION = $(shell SRC_DIR=prism/src prism/src/scripts/printversion.sh 2> /dev/null)

# Build a source distribution
dist_src: version
	mkdir dontcopy
	@if [ -e prism/examples ]; then \
	  echo "mv prism/examples dontcopy"; mv prism/examples dontcopy; \
	fi
	@if [ -e prism/tests ]; then \
	  echo "mv prism/tests dontcopy"; mv prism/tests dontcopy; \
	fi
	mv prism-examples prism/examples
	mv cudd prism
	mv prism "prism-$(VERSION)-src"
	(cd "prism-$(VERSION)-src"; $(MAKE) dist_src VERSION=$(VERSION))
	tar cfz "prism-$(VERSION)-src.tar.gz" --exclude=.svn "prism-$(VERSION)-src"

# Display version
version:
	@echo VERSION = $(VERSION)

#################################################
