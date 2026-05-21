package com.gorib.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gorib.app.data.db.dao.BudgetDao
import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.RecurringBillDao
import com.gorib.app.data.db.dao.TransactionDao
import com.gorib.app.data.db.dao.RentConfigDao
import com.gorib.app.data.db.dao.UtilityBillGroupDao
import com.gorib.app.data.db.dao.GroceryItemDao
import com.gorib.app.data.db.dao.BrandMappingDao
import com.gorib.app.data.db.entity.BudgetEntity
import com.gorib.app.data.db.entity.CategoryEntity
import com.gorib.app.data.db.entity.RecurringBillEntity
import com.gorib.app.data.db.entity.TransactionEntity
import com.gorib.app.data.db.entity.RentConfigEntity
import com.gorib.app.data.db.entity.UtilityBillGroupEntity
import com.gorib.app.data.db.entity.UtilityBillLineItemEntity
import com.gorib.app.data.db.entity.GroceryItemEntity
import com.gorib.app.data.db.entity.BrandMappingEntity
import com.gorib.app.data.db.dao.CategoryKeywordOverrideDao
import com.gorib.app.data.db.entity.CategoryKeywordOverrideEntity
import com.gorib.app.data.db.dao.ShoppingSessionDao
import com.gorib.app.data.db.dao.ShoppingSessionItemDao
import com.gorib.app.data.db.entity.ShoppingSessionEntity
import com.gorib.app.data.db.entity.ShoppingSessionItemEntity
import com.gorib.app.data.db.dao.RecurringExpenseDao
import com.gorib.app.data.db.entity.RecurringExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Main Room database for the GORIB application.
 * Manages all entities and exposes abstract DAO accessors.
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        RecurringBillEntity::class,
        RentConfigEntity::class,
        UtilityBillGroupEntity::class,
        UtilityBillLineItemEntity::class,
        GroceryItemEntity::class,
        BrandMappingEntity::class,
        CategoryKeywordOverrideEntity::class,
        ShoppingSessionEntity::class,
        ShoppingSessionItemEntity::class,
        RecurringExpenseEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class GoribDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringBillDao(): RecurringBillDao
    abstract fun rentConfigDao(): RentConfigDao
    abstract fun utilityBillGroupDao(): UtilityBillGroupDao
    abstract fun groceryItemDao(): GroceryItemDao
    abstract fun brandMappingDao(): BrandMappingDao
    abstract fun categoryKeywordOverrideDao(): CategoryKeywordOverrideDao
    abstract fun shoppingSessionDao(): ShoppingSessionDao
    abstract fun shoppingSessionItemDao(): ShoppingSessionItemDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao

    companion object {
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recurring_expense` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `amount_rm` REAL NOT NULL, 
                        `category_id` INTEGER NOT NULL, 
                        `due_day` INTEGER NOT NULL, 
                        `is_active` INTEGER NOT NULL, 
                        `note` TEXT, 
                        `created_at` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `receipt_path` TEXT")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE `transactions` ADD COLUMN `receipt_path` TEXT")
                } catch (e: Exception) {}
            }
        }

        @Volatile
        private var INSTANCE: GoribDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GoribDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoribDatabase::class.java,
                    "gorib_database"
                )
                    .addCallback(GoribDatabaseCallback(scope))
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class GoribDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Categories — use INSERT OR REPLACE to guarantee seed always runs
                val categories = listOf(
                    Triple("Groceries",        "🛒", 1),
                    Triple("Outside Food",     "🍜", 2),
                    Triple("Fuel & Transport", "⛽", 3),
                    Triple("Online Shopping",  "🛍️", 4),
                    Triple("Medical",          "💊", 5),
                    Triple("Entertainment",    "🎮", 6),
                    Triple("Personal Care",    "🪥", 7),
                    Triple("Clothing",         "👕", 8),
                    Triple("Others",           "📦", 9),
                    Triple("Subscriptions",    "📱", 10)
                )
                categories.forEach { (name, emoji, sortOrder) ->
                    db.execSQL("""
                        INSERT OR REPLACE INTO categories
                        (name, icon_emoji, is_system, sort_order, type, monthly_budget_rm)
                        VALUES ('$name', '$emoji', 1, $sortOrder, 'EXPENSE', NULL)
                    """)
                }

                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDefaultGroceryItems(database.groceryItemDao())
                        populateDefaultBrandMappings(database.brandMappingDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Safety net: if categories table is empty, re-seed
                val cursor = db.query("SELECT COUNT(*) FROM categories")
                cursor.moveToFirst()
                val count = cursor.getInt(0)
                cursor.close()
                if (count == 0) {
                    val categories = listOf(
                        Triple("Groceries",        "🛒", 1),
                        Triple("Outside Food",     "🍜", 2),
                        Triple("Fuel & Transport", "⛽", 3),
                        Triple("Online Shopping",  "🛍️", 4),
                        Triple("Medical",          "💊", 5),
                        Triple("Entertainment",    "🎮", 6),
                        Triple("Personal Care",    "🪥", 7),
                        Triple("Clothing",         "👕", 8),
                        Triple("Others",           "📦", 9),
                        Triple("Subscriptions",    "📱", 10)
                    )
                    categories.forEach { (name, emoji, sortOrder) ->
                        db.execSQL("""
                            INSERT OR REPLACE INTO categories
                            (name, icon_emoji, is_system, sort_order, type, monthly_budget_rm)
                            VALUES ('$name', '$emoji', 1, $sortOrder, 'EXPENSE', NULL)
                        """)
                    }
                }
            }

            private suspend fun populateDefaultGroceryItems(groceryItemDao: GroceryItemDao) {
                val items = listOf(
                    // VEGETABLES (25 items)
                    GroceryItemEntity(name = "Red Onion", groceryCategory = "VEGETABLE", aliases = "bawang merah,BWNG MERAH,red onion", defaultUnit = "kg"),
                    GroceryItemEntity(name = "White Onion", groceryCategory = "VEGETABLE", aliases = "bawang putih besar,white onion", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Garlic", groceryCategory = "VEGETABLE", aliases = "bawang putih,garlic,garlic bulb", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Ginger", groceryCategory = "VEGETABLE", aliases = "halia,ginger,halia fresh", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Potato", groceryCategory = "VEGETABLE", aliases = "kentang,potato,potatoes", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Tomato", groceryCategory = "VEGETABLE", aliases = "tomato,tomatoes,tomat", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Carrot", groceryCategory = "VEGETABLE", aliases = "lobak merah,carrot,carrots", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Cabbage", groceryCategory = "VEGETABLE", aliases = "kobis,cabbage,kol", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Spinach", groceryCategory = "VEGETABLE", aliases = "bayam,spinach,bayam hijau", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Kangkung", groceryCategory = "VEGETABLE", aliases = "kangkung,water spinach,morning glory", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Broccoli", groceryCategory = "VEGETABLE", aliases = "brokoli,broccoli", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Cauliflower", groceryCategory = "VEGETABLE", aliases = "bunga kobis,cauliflower", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Bitter Gourd", groceryCategory = "VEGETABLE", aliases = "peria,bitter gourd,peria katak", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Long Bean", groceryCategory = "VEGETABLE", aliases = "kacang panjang,long bean", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Lady Finger", groceryCategory = "VEGETABLE", aliases = "bendi,okra,lady finger,ladyfinger", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Chili", groceryCategory = "VEGETABLE", aliases = "cili,chili,chilli,cili merah", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Turmeric", groceryCategory = "VEGETABLE", aliases = "kunyit,turmeric,kunyit fresh", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Lemongrass", groceryCategory = "VEGETABLE", aliases = "serai,lemongrass,lemon grass", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Pandan Leaf", groceryCategory = "VEGETABLE", aliases = "daun pandan,pandan leaf,pandan", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Curry Leaf", groceryCategory = "VEGETABLE", aliases = "daun kari,curry leaf,curry leaves", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Cucumber", groceryCategory = "VEGETABLE", aliases = "timun,cucumber", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Eggplant", groceryCategory = "VEGETABLE", aliases = "terung,brinjal,eggplant,aubergine", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Corn", groceryCategory = "VEGETABLE", aliases = "jagung,corn,sweet corn", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Mushroom", groceryCategory = "VEGETABLE", aliases = "cendawan,mushroom,mushrooms", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Celery", groceryCategory = "VEGETABLE", aliases = "saderi,celery", defaultUnit = "pack"),

                    // FRUITS (12 items)
                    GroceryItemEntity(name = "Banana", groceryCategory = "FRUIT", aliases = "pisang,banana,bananas", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Apple", groceryCategory = "FRUIT", aliases = "epal,apple,apples", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Orange", groceryCategory = "FRUIT", aliases = "oren,orange,oranges", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Mango", groceryCategory = "FRUIT", aliases = "mangga,mango", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Watermelon", groceryCategory = "FRUIT", aliases = "tembikai,watermelon", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Papaya", groceryCategory = "FRUIT", aliases = "betik,papaya", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Guava", groceryCategory = "FRUIT", aliases = "jambu batu,guava", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Dragon Fruit", groceryCategory = "FRUIT", aliases = "pitaya,dragon fruit,buah naga", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Rambutan", groceryCategory = "FRUIT", aliases = "rambutan", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Grapes", groceryCategory = "FRUIT", aliases = "anggur,grapes,grape", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Pineapple", groceryCategory = "FRUIT", aliases = "nanas,pineapple", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Coconut", groceryCategory = "FRUIT", aliases = "kelapa,coconut", defaultUnit = "pcs"),

                    // MEAT & SEAFOOD (12 items)
                    GroceryItemEntity(name = "Chicken Whole", groceryCategory = "MEAT", aliases = "ayam,whole chicken,ayam segar", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Chicken Fillet", groceryCategory = "MEAT", aliases = "ayamas,chicken breast,fillet ayam,CHICK FILLET", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Chicken Drumstick", groceryCategory = "MEAT", aliases = "drumstick,peha ayam,chicken leg", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Chicken Wings", groceryCategory = "MEAT", aliases = "sayap ayam,chicken wings,wings", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Beef", groceryCategory = "MEAT", aliases = "daging lembu,beef,daging", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Mutton", groceryCategory = "MEAT", aliases = "daging kambing,mutton,lamb", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Mackerel", groceryCategory = "MEAT", aliases = "ikan kembung,mackerel,kembung", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Catfish", groceryCategory = "MEAT", aliases = "ikan keli,catfish,keli", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Sardine", groceryCategory = "MEAT", aliases = "ikan sardin,sardine", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Prawns", groceryCategory = "MEAT", aliases = "udang,prawns,shrimp,prawn", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Squid", groceryCategory = "MEAT", aliases = "sotong,squid,calamari", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Chicken Egg", groceryCategory = "MEAT", aliases = "telur,egg,eggs,telur ayam", defaultUnit = "tray"),

                    // DAIRY (8 items)
                    GroceryItemEntity(name = "Fresh Milk", groceryCategory = "DAIRY", aliases = "susu segar,fresh milk,SUSU SEGAR,susu", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "UHT Milk", groceryCategory = "DAIRY", aliases = "susu uht,uht milk,long life milk", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Condensed Milk", groceryCategory = "DAIRY", aliases = "susu pekat,condensed milk,susu manis", defaultUnit = "tin"),
                    GroceryItemEntity(name = "Evaporated Milk", groceryCategory = "DAIRY", aliases = "susu cair,evaporated milk,ideal milk", defaultUnit = "tin"),
                    GroceryItemEntity(name = "Yogurt", groceryCategory = "DAIRY", aliases = "yogurt,yoghurt", defaultUnit = "cup"),
                    GroceryItemEntity(name = "Butter", groceryCategory = "DAIRY", aliases = "mentega,butter", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Cheese", groceryCategory = "DAIRY", aliases = "keju,cheese", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Coconut Milk", groceryCategory = "DAIRY", aliases = "santan,coconut milk,coconut cream", defaultUnit = "pack"),

                    // DRY GOODS (11 items)
                    GroceryItemEntity(name = "Rice", groceryCategory = "DRY_GOODS", aliases = "beras,rice,nasi,BERAS WANGI,CAP SAUH", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Bread", groceryCategory = "DRY_GOODS", aliases = "roti,bread,roti putih,GARDENIA,HIGH5,MASSIMO", defaultUnit = "loaf"),
                    GroceryItemEntity(name = "Flour", groceryCategory = "DRY_GOODS", aliases = "tepung,flour,tepung gandum", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Sugar", groceryCategory = "DRY_GOODS", aliases = "gula,sugar,gula putih,BESTARI", defaultUnit = "kg"),
                    GroceryItemEntity(name = "Salt", groceryCategory = "DRY_GOODS", aliases = "garam,salt", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Cooking Oil", groceryCategory = "DRY_GOODS", aliases = "minyak masak,cooking oil,minyak,NATUREL,KNIFE,SERI MURNI", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Yellow Noodles", groceryCategory = "DRY_GOODS", aliases = "mee,yellow noodles,mee kuning", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Rice Noodles", groceryCategory = "DRY_GOODS", aliases = "bihun,rice noodles,vermicelli", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Instant Noodles", groceryCategory = "DRY_GOODS", aliases = "maggi,mee segera,instant noodle,MAGGI", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Oats", groceryCategory = "DRY_GOODS", aliases = "oat,oats,quaker,oatmeal", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Lentils", groceryCategory = "DRY_GOODS", aliases = "dal,dhal,lentils,kacang dal", defaultUnit = "pack"),

                    // BEVERAGES (6 items)
                    GroceryItemEntity(name = "Mineral Water", groceryCategory = "BEVERAGE", aliases = "air mineral,mineral water,spritzer,SPRITZER", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Milo", groceryCategory = "BEVERAGE", aliases = "milo,MILO,chocolate malt", defaultUnit = "tin"),
                    GroceryItemEntity(name = "Nescafe", groceryCategory = "BEVERAGE", aliases = "nescafe,NESCAFE,coffee powder,kopi", defaultUnit = "jar"),
                    GroceryItemEntity(name = "Tea", groceryCategory = "BEVERAGE", aliases = "teh,tea,boh,BOH,lipton,LIPTON,CAP WELDNA", defaultUnit = "box"),
                    GroceryItemEntity(name = "Soy Milk", groceryCategory = "BEVERAGE", aliases = "susu soya,soy milk,vitasoy", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Fruit Juice", groceryCategory = "BEVERAGE", aliases = "jus buah,fruit juice,orange juice", defaultUnit = "bottle"),

                    // SPICES & CONDIMENTS (11 items)
                    GroceryItemEntity(name = "Coriander Powder", groceryCategory = "SPICE", aliases = "serbuk ketumbar,coriander powder", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Cumin", groceryCategory = "SPICE", aliases = "jintan putih,cumin,jeera", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Turmeric Powder", groceryCategory = "SPICE", aliases = "serbuk kunyit,turmeric powder", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Chili Powder", groceryCategory = "SPICE", aliases = "serbuk cili,chili powder,cayenne", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Black Pepper", groceryCategory = "SPICE", aliases = "lada hitam,black pepper", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Soy Sauce", groceryCategory = "SPICE", aliases = "kicap,soy sauce,kicap manis", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Oyster Sauce", groceryCategory = "SPICE", aliases = "sos tiram,oyster sauce", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Fish Sauce", groceryCategory = "SPICE", aliases = "sos ikan,fish sauce,budu", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Chili Sauce", groceryCategory = "SPICE", aliases = "sos cili,chili sauce,sriracha", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Tomato Sauce", groceryCategory = "SPICE", aliases = "sos tomato,tomato sauce,ketchup", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Belacan", groceryCategory = "SPICE", aliases = "belacan,shrimp paste,prawn paste", defaultUnit = "pack"),

                    // CLEANING (7 items)
                    GroceryItemEntity(name = "Laundry Detergent", groceryCategory = "CLEANING", aliases = "sabun basuh,detergent,washing powder,FAB,BREEZE", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Dish Soap", groceryCategory = "CLEANING", aliases = "sabun pinggan,dish soap,sunlight,SUNLIGHT", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Floor Cleaner", groceryCategory = "CLEANING", aliases = "pencuci lantai,floor cleaner,dettol floor", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Toilet Cleaner", groceryCategory = "CLEANING", aliases = "pencuci tandas,toilet cleaner,harpic,HARPIC", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Fabric Softener", groceryCategory = "CLEANING", aliases = "pelembut kain,fabric softener,comfort,COMFORT", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Garbage Bags", groceryCategory = "CLEANING", aliases = "beg sampah,garbage bag,trash bag", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Tissue Paper", groceryCategory = "CLEANING", aliases = "tisu,tissue,toilet paper,tisu tandas", defaultUnit = "pack"),

                    // PERSONAL CARE (7 items)
                    GroceryItemEntity(name = "Shampoo", groceryCategory = "PERSONAL_CARE", aliases = "syampu,shampoo,HEAD SHOULDERS,PANTENE", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Body Wash", groceryCategory = "PERSONAL_CARE", aliases = "sabun mandi,body wash,shower gel,DETTOL", defaultUnit = "bottle"),
                    GroceryItemEntity(name = "Toothpaste", groceryCategory = "PERSONAL_CARE", aliases = "ubat gigi,toothpaste,COLGATE,DARLIE", defaultUnit = "tube"),
                    GroceryItemEntity(name = "Toothbrush", groceryCategory = "PERSONAL_CARE", aliases = "berus gigi,toothbrush", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Soap Bar", groceryCategory = "PERSONAL_CARE", aliases = "sabun batang,soap bar,DETTOL BAR,LIFEBUOY", defaultUnit = "pcs"),
                    GroceryItemEntity(name = "Sanitary Pad", groceryCategory = "PERSONAL_CARE", aliases = "tuala wanita,sanitary pad,pad,LAURIER,KOTEX", defaultUnit = "pack"),
                    GroceryItemEntity(name = "Lotion", groceryCategory = "PERSONAL_CARE", aliases = "losyen,lotion,body lotion,VASELINE", defaultUnit = "bottle")
                )
                groceryItemDao.insertGroceryItems(items)
            }

            private suspend fun populateDefaultBrandMappings(brandMappingDao: BrandMappingDao) {
                val mappings = listOf(
                    BrandMappingEntity(rawText = "AYAMAS CHICK FILLET", mappedItemName = "Chicken Fillet", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "AYAMAS DRUM STICK", mappedItemName = "Chicken Drumstick", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "AYAMAS CHICKEN WING", mappedItemName = "Chicken Wings", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "F&N SUSU SEGAR", mappedItemName = "Fresh Milk", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "DUTCH LADY FULL CREAM", mappedItemName = "Fresh Milk", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "DUTCH LADY UHT", mappedItemName = "UHT Milk", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "MAGNOLIA FRESH MILK", mappedItemName = "Fresh Milk", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "FARMERS FRESH MILK", mappedItemName = "Fresh Milk", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "NATUREL COOKING OIL", mappedItemName = "Cooking Oil", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "KNIFE COOKING OIL", mappedItemName = "Cooking Oil", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "SERI MURNI MINYAK", mappedItemName = "Cooking Oil", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "BERAS WANGI 5KG", mappedItemName = "Rice", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "CAP SAUH BERAS", mappedItemName = "Rice", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "JASMINE RICE", mappedItemName = "Rice", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "GARDENIA BREAD", mappedItemName = "Bread", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "HIGH5 BREAD", mappedItemName = "Bread", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "MASSIMO BREAD", mappedItemName = "Bread", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "MILO ACTIV-GO", mappedItemName = "Milo", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "NESCAFE CLASSIC", mappedItemName = "Nescafe", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "BOH TEA", mappedItemName = "Tea", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "LIPTON TEA", mappedItemName = "Tea", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "MAGGI MEE GORENG", mappedItemName = "Instant Noodles", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "MAGGI 2MIN NOODLES", mappedItemName = "Instant Noodles", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "BESTARI GULA", mappedItemName = "Sugar", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "SUNQUICK CORDIAL", mappedItemName = "Fruit Juice", confidence = 0.85, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "SPRITZER WATER", mappedItemName = "Mineral Water", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "COLGATE TOOTHPASTE", mappedItemName = "Toothpaste", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "DARLIE TOOTHPASTE", mappedItemName = "Toothpaste", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "DETTOL SHOWER GEL", mappedItemName = "Body Wash", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "DETTOL SOAP", mappedItemName = "Soap Bar", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "SUNLIGHT DISH WASH", mappedItemName = "Dish Soap", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "BREEZE DETERGENT", mappedItemName = "Laundry Detergent", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "HARPIC TOILET", mappedItemName = "Toilet Cleaner", confidence = 0.95, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "COMFORT FABRIC", mappedItemName = "Fabric Softener", confidence = 0.90, isUserConfirmed = false),
                    BrandMappingEntity(rawText = "LAURIER SANITARY", mappedItemName = "Sanitary Pad", confidence = 0.95, isUserConfirmed = false)
                )
                brandMappingDao.insertBrandMappings(mappings)
            }
        }
    }
}
