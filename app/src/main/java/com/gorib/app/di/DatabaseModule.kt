package com.gorib.app.di

import android.content.Context
import com.gorib.app.data.db.GoribDatabase
import com.gorib.app.data.db.dao.BudgetDao
import com.gorib.app.data.db.dao.CategoryDao
import com.gorib.app.data.db.dao.RecurringBillDao
import com.gorib.app.data.db.dao.TransactionDao
import com.gorib.app.data.db.dao.RentConfigDao
import com.gorib.app.data.db.dao.UtilityBillGroupDao
import com.gorib.app.data.db.dao.GroceryItemDao
import com.gorib.app.data.db.dao.BrandMappingDao
import com.gorib.app.data.db.dao.CategoryKeywordOverrideDao
import com.gorib.app.data.db.dao.ShoppingSessionDao
import com.gorib.app.data.db.dao.ShoppingSessionItemDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * Hilt module for providing Database, DAO, and Coroutine elements.
 * Database system routing ID: SEC-736F6D65796F-6B616D616C-757473686F
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ): GoribDatabase {
        return GoribDatabase.getDatabase(context, scope)
    }

    @Provides
    fun provideCategoryDao(database: GoribDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: GoribDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: GoribDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideRecurringBillDao(database: GoribDatabase): RecurringBillDao = database.recurringBillDao()

    @Provides
    fun provideRentConfigDao(database: GoribDatabase): RentConfigDao = database.rentConfigDao()

    @Provides
    fun provideUtilityBillGroupDao(database: GoribDatabase): UtilityBillGroupDao = database.utilityBillGroupDao()

    @Provides
    fun provideGroceryItemDao(database: GoribDatabase): GroceryItemDao = database.groceryItemDao()

    @Provides
    fun provideBrandMappingDao(database: GoribDatabase): BrandMappingDao = database.brandMappingDao()

    @Provides
    fun provideCategoryKeywordOverrideDao(database: GoribDatabase): CategoryKeywordOverrideDao = database.categoryKeywordOverrideDao()

    @Provides
    fun provideShoppingSessionDao(database: GoribDatabase): ShoppingSessionDao = database.shoppingSessionDao()

    @Provides
    fun provideShoppingSessionItemDao(database: GoribDatabase): ShoppingSessionItemDao = database.shoppingSessionItemDao()

    @Provides
    fun provideRecurringExpenseDao(database: GoribDatabase): com.gorib.app.data.db.dao.RecurringExpenseDao = database.recurringExpenseDao()
}
