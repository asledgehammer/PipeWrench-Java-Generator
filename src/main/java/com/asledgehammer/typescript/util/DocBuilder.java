package com.asledgehammer.typescript.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DocBuilder {

  private final List<String> lines = new ArrayList<>();
  private final String prefix;

  public DocBuilder() {
    this("");
  }

  public DocBuilder(String prefix) {
    this.prefix = prefix;
  }

  @Override
  public String toString() {
    return build(prefix);
  }

  public DocBuilder appendLine(String... lines) {
    if (lines.length == 0) {
      this.lines.add("");
      return this;
    }
    Collections.addAll(this.lines, lines);
    return this;
  }

  public DocBuilder appendReturn(String description) {
    appendLine("@returns " + description);
    return this;
  }

  public DocBuilder appendParam(String name, String description) {
    appendLine("@param " + name + " " + description);
    return this;
  }

  public String build(String prefix) {

    if (isEmpty()) {
      return prefix + "/** */";
    } else if (lines.size() == 1) {
      return prefix + "/** " + lines.get(0) + " */";
    }

    StringBuilder builder = new StringBuilder(prefix).append("/**\n");
    for (String line : lines) {
      if (line == null || line.isEmpty()) {
        builder.append(prefix).append(" *\n");
        continue;
      }
      builder.append(prefix).append(" * ").append(line).append('\n');
    }
    builder.append(prefix).append(" */");
    return builder.toString();
  }

  public boolean isEmpty() {
    return lines.isEmpty();
  }
}
