package ED;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "ED")
public class DamageTracker {

    // 存储所有的伤害记录
    public static List<DamageRecord> GLOBAL_RECORDS = new ArrayList<>();
    // 存储致命一击的记录
    public static DamageRecord GLOBAL_FATAL_BLOW = null;

    /**
     * 【内部类】伤害记录
     * 严格保留 ClientDamageDisplay 期望的所有字段名！
     * 你 UI 里怎么写的，这里就怎么定义，绝不改动！
     */
    public static class DamageRecord {
        public String playerName;      // 受害者名字
        public String attackerName;    // 攻击者名字
        public float damage;           // 伤害数值
        public float playerHealth;     // 受害者当前血量
        public float playerMaxHealth;  // 受害者最大血量
        public float attackerHealth;   // 攻击者当前血量
        public float attackerMaxHealth;// 攻击者最大血量

        public DamageRecord(String pN, String aN, float d, float pH, float pMH, float aH, float aMH) {
            this.playerName = pN;
            this.attackerName = aN;
            this.damage = d;
            this.playerHealth = pH;
            this.playerMaxHealth = pMH;
            this.attackerHealth = aH;
            this.attackerMaxHealth = aMH;
        }
    }

    /**
     * 核心逻辑：获取真正的攻击者（兼容 TACZ 等枪械模组）
     */
    private static LivingEntity getTrueAttacker(Entity source) {
        if (source instanceof LivingEntity) {
            return (LivingEntity) source;
        }
        if (source instanceof Projectile) {
            Entity owner = ((Projectile) source).getOwner();
            if (owner instanceof LivingEntity) {
                return (LivingEntity) owner;
            }
        }
        return null;
    }

    /**
     * 监听受伤事件：记录每一发子弹的伤害
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        LivingEntity trueAttacker = getTrueAttacker(event.getSource().getDirectEntity());
        if (trueAttacker == null) return; // 没有攻击者就不记录

        float currentHp = player.getHealth();
        float maxHp = player.getMaxHealth();
        float dmg = event.getAmount();

        // 先记录伤害，再判断是否致死
        DamageRecord record = new DamageRecord(
                player.getName().getString(),
                trueAttacker.getName().getString(),
                dmg,
                currentHp,
                maxHp,
                trueAttacker.getHealth(),
                trueAttacker.getMaxHealth()
        );

        // 判断是否致死
        if (dmg >= currentHp) {
            GLOBAL_FATAL_BLOW = record;
        } else {
            GLOBAL_RECORDS.add(record);
        }
    }

    /**
     * 监听死亡事件
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        // 死亡时不做任何清空操作，保留数据供死亡界面显示
    }
}