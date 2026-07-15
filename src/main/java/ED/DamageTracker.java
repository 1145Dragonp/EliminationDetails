package ED;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = mainclass.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamageTracker {

    private static final Logger LOGGER = LogManager.getLogger("DamageTracker");

    // 全局共享变量
    public static final CopyOnWriteArrayList<DamageRecord> GLOBAL_RECORDS = new CopyOnWriteArrayList<>();
    public static volatile DamageRecord GLOBAL_FATAL_BLOW = null;

    public static class DamageRecord {
        public String playerName, attackerName;
        public float damage, playerHealth, playerMaxHealth, attackerHealth, attackerMaxHealth;

        public DamageRecord(String pN, String aN, float d, float pH, float pMH, float aH, float aMH) {
            this.playerName = pN; this.attackerName = aN; this.damage = d;
            this.playerHealth = pH; this.playerMaxHealth = pMH;
            this.attackerHealth = aH; this.attackerMaxHealth = aMH;
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 【日志1】只要有人受伤，必定打印！
        //LOGGER.info("[DamageTracker] Hurt event triggered! Entity: {}", event.getEntity().getName().getString());

        // 判断是否是玩家
        if (!(event.getEntity() instanceof Player player)) {
            //LOGGER.info("[DamageTracker] Not a player, ignoring.");
            return;
        }

        // 获取伤害来源
        LivingEntity attacker = (event.getSource().getEntity() instanceof LivingEntity)
                ? (LivingEntity) event.getSource().getEntity() : null;

        // 【日志2】打印攻击者信息
        if (attacker == null) {
            //LOGGER.info("[DamageTracker] No living attacker (e.g., fall damage), ignoring.");
            return;
        }
        //LOGGER.info("[DamageTracker] Attacker: {}", attacker.getName().getString());

        float currentHp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float dmg = event.getAmount();

        // 【日志3】打印血量变化
        //LOGGER.info("[DamageTracker] Player HP: {}/{}, Damage: {}", currentHp, maxHp, dmg);

        // 满血重置
        if (currentHp >= maxHp) {
            GLOBAL_RECORDS.clear();
            GLOBAL_FATAL_BLOW = null;
            //LOGGER.info("[DamageTracker] Full health, records cleared.");
        }

        // 记录伤害
        if (currentHp < maxHp) {
            DamageRecord record = new DamageRecord(
                    player.getName().getString(), attacker.getName().getString(),
                    dmg, currentHp, maxHp, attacker.getHealth(), attacker.getMaxHealth()
            );
            if (dmg >= currentHp) {
                GLOBAL_FATAL_BLOW = record;
               // LOGGER.info("[DamageTracker] FATAL BLOW recorded!");
            } else {
                GLOBAL_RECORDS.add(record);
                //LOGGER.info("[DamageTracker] Normal hit recorded. Total records: {}", GLOBAL_RECORDS.size());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 【日志4】死亡事件
       // LOGGER.info("[DamageTracker] Death event triggered! Entity: {}", event.getEntity().getName().getString());
        if (!(event.getEntity() instanceof Player)) return;

        //LOGGER.info("[DamageTracker] Player died! Global records count: {}", GLOBAL_RECORDS.size());
        if (GLOBAL_FATAL_BLOW != null) {
           // LOGGER.info("[DamageTracker] Fatal blow was by: {}", GLOBAL_FATAL_BLOW.attackerName);
        }
    }
}