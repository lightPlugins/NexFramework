package io.nexstudios.framework.data.service.language.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "nex_player_locales")
public class PlayerLocaleEntity {

  @Id
  @Column(name = "player_uuid", length = 36, nullable = false)
  private String playerUuid;

  @Column(name = "locale_tag", length = 32, nullable = false)
  private String localeTag;

  protected PlayerLocaleEntity() {
    // JPA
  }

  public PlayerLocaleEntity(String playerUuid, String localeTag) {
    this.playerUuid = playerUuid;
    this.localeTag = localeTag;
  }
}