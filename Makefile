all: run

clean:
	rm -f out/WordCount.jar

out/WordCount.jar: out/parcs.jar src/WordCount.java
	@mkdir -p temp
	@javac -cp out/parcs.jar -d temp src/WordCount.java
	@jar cf out/WordCount.jar -C temp .
	@rm -rf temp/

build: out/WordCount.jar

run: out/WordCount.jar
	@cd out && java -cp 'parcs.jar:WordCount.jar' WordCount $(WORKERS)
