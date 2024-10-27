<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class ListItems extends Model
{
    protected $fillable = [
        'id', 
        'category_id', 
        'title', 
        'is_completed'
        ];

    public function categories(){
        return $this->belongsTo(Categories::class, 'category_id');
    }
}
