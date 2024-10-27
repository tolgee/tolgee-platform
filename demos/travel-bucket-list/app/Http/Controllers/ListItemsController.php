<?php

namespace App\Http\Controllers;

use App\Models\Categories;
use App\Models\ListItems;
use Illuminate\Http\Request;

class ListItemsController extends Controller
{
    /**
     * Display a listing of the resource.
     */
    public function index()
    {
        $categories = Categories::all();
        $currentDate = \Carbon\Carbon::now();
        $startOfDay = $currentDate->copy()->startOfDay(); // 00:00:00
        $endOfDay = $currentDate->copy()->endOfDay(); // 23:59:59
        $listItems = ListItems::with('categories')->whereBetween('created_at', [$startOfDay, $endOfDay])->get();        
        return view('index', compact('categories','listItems'));
    }

    /**
     * Show the form for creating a new resource.
     */
    public function create()
    {
        //
    }

    /**
     * Store a newly created resource in storage.
     */
    public function store(Request $request)
    {
        $validated = $request->validate([
            'item' => 'required',
            'category' => 'required',
        ]);
        ListItems::create([
            'category_id' => $validated['category'],
            'title' => $validated['item'],
        ]);

        return redirect()->back()->with(['success' => 'Item added successfully', 'class' => 'show']);
    }

    /**
     * Display the specified resource.
     */
    public function show(ListItems $listItems)
    {
        //
    }

    /**
     * Show the form for editing the specified resource.
     */
    public function editListItem(Request $request, ListItems $listItems)
    {
        $ListItems = ListItems::with('categories')->where('id', $request->id)->first();
        return response()->json(['status' => 200, 'listItem' => $ListItems]);
    }

    /**
     * Update the specified resource in storage.
     */
    public function update(Request $request, ListItems $listItems)
    {
        ListItems::where('id', $request->itemId)->update(['title' => $request->itemTitle]);
        return redirect()->back()->with(['success' => 'Item updated successfully', 'class' => 'show']);
    }

    /**
     * Remove the specified resource from storage.
     */
    public function destroy(ListItems $listItems)
    {
        //
    }

    public function updateStatus(Request $request){
        $listItemData = ListItems::where('id', $request->listId)->first();
        $isCompleted = ($listItemData->is_completed == '1') ? '0' : '1';
        $status = ($isCompleted)? true : false;
        $message = ($status)? 'Item marked as completed' : 'Item marked as incomplete';
        $ListItems = ListItems::where('id', $request->listId)->update(['is_completed' => $isCompleted]);
        return response()->json(['success' => 'Item updated', 'message' => $message, 'status' => $status, 'id' => $listItemData->id]);
    }
    public function listItemDateWise(Request $request){
        
        $currentDate = \Carbon\Carbon::parse($request->currentDate);
        
        $startOfDay = $currentDate->copy()->startOfDay();
        $endOfDay = $currentDate->copy()->endOfDay();
        $listItems = ListItems::with('categories')->whereBetween('created_at', [$startOfDay, $endOfDay])->get();
        if(count($listItems))
            return response()->json(['status' => 200,'listItems' => $listItems]);
        else
            return response()->json(['status' => 401]);    }

    public function filterByCategory(Request $request){
        $listItems = ListItems::with('categories')->where('category_id', $request->categoryFilter)->get();
        if(count($listItems))
            return response()->json(['status' => 200,'listItems' => $listItems]);
        else
            return response()->json(['status' => 401]);
    }
}
