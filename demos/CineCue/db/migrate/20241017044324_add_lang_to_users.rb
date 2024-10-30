class AddLangToUsers < ActiveRecord::Migration[7.1]
  def change
    add_column :users, :lang, :string
  end
end
