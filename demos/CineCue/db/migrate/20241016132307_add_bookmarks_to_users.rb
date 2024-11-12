class AddBookmarksToUsers < ActiveRecord::Migration[7.1]
  def change
    add_column :users, :bookmarks, :string
  end
end
