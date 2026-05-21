package com.gorib.app.di

import com.gorib.app.data.repository.*
import com.gorib.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindRecurringBillRepository(impl: RecurringBillRepositoryImpl): RecurringBillRepository

    @Binds
    @Singleton
    abstract fun bindRentConfigRepository(impl: RentConfigRepositoryImpl): RentConfigRepository

    @Binds
    @Singleton
    abstract fun bindUtilityRepository(impl: UtilityRepositoryImpl): UtilityRepository

    @Binds
    @Singleton
    abstract fun bindGroceryRepository(impl: GroceryRepositoryImpl): GroceryRepository

    @Binds
    @Singleton
    abstract fun bindRecurringExpenseRepository(impl: RecurringExpenseRepositoryImpl): RecurringExpenseRepository
}
