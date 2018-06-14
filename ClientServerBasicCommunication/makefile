JCC = javac


# the -g flag compiles with debugging information
#
JFLAGS = -g

# typing 'make' will invoke the first target entry in the makefile 
#
default: sender.class receiver.class
sender.class: sender.java
	$(JCC) $(JFLAGS) sender.java

receiver.class: receiver.java
	$(JCC) $(JFLAGS) receiver.java

# To start over from scratch, type 'make clean'.  
#
clean:
	$(RM) *.class