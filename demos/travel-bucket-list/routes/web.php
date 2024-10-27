<?php

use App\Http\Controllers\CategoriesController;
use App\Http\Controllers\ListItemsController;
use Illuminate\Support\Facades\Route;

Route::get('/', [ListItemsController::class, 'index']);
Route::resource('category', CategoriesController::class);
Route::resource('listItem', ListItemsController::class);
Route::post('listItemDateWise', [ListItemsController::class, 'listItemDateWise'])->name('show.listItem.dateWise');
Route::post('filterByCategory', [ListItemsController::class, 'filterByCategory'])->name('show.listItem.categoryWise');
Route::post('updateStatus', [ListItemsController::class, 'updateStatus'])->name('update.listItem.status');
Route::get('updateListItem', [ListItemsController::class, 'update'])->name('update.listItem');
Route::post('editListItem', [ListItemsController::class, 'editListItem'])->name('edit.listItem');

