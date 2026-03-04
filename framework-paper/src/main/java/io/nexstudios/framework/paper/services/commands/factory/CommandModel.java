package io.nexstudios.framework.paper.services.commands.factory;


import io.nexstudios.framework.paper.services.commands.annotations.CommandRoot;
import io.nexstudios.framework.paper.services.commands.factory.model.CmdNode;

import java.util.Objects;

public record CommandModel(CommandRoot rootAnn, CmdNode root) {
  public CommandModel {
    Objects.requireNonNull(rootAnn, "rootAnn");
    Objects.requireNonNull(root, "root");
  }
}